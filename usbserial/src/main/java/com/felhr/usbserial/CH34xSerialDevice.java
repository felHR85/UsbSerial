/*
 * Heavily based on a pull-request made by Andreas Butti to https://github.com/mik3y/usb-serial-for-android
 * https://github.com/mik3y/usb-serial-for-android/pull/92
 * 
 * Update May 9 2015: First tests appear to be working. No error messages are received when config the chip
 * Thanks to Paul Alcock for provide me with one of those Arduino nano clones!!!
 * */

package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public class CH34xSerialDevice extends UsbSerialDevice
{
    private static final String CLASS_ID = CH34xSerialDevice.class.getSimpleName();

    private static final int DEFAULT_BAUDRATE = 9600;

    private static final int REQTYPE_HOST_FROM_DEVICE = UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_IN;
    private static final int REQTYPE_HOST_TO_DEVICE = 0x41;

    private static final int CH341_REQ_WRITE_REG = 0x9A;
    private static final int CH341_REQ_READ_REG = 0x95;
    private static final int CH341_REG_BREAK1 = 0x05;
    private static final int CH341_REG_BREAK2 = 0x18;
    private static final int CH341_NBREAK_BITS_REG1 = 0x01;
    private static final int CH341_NBREAK_BITS_REG2 = 0x40;

    private static final int CH34X_2400_1 = 0xd901;
    private static final int CH34X_2400_2 = 0x0038;
    private static final int CH34X_4800_1 = 0x6402;
    private static final int CH34X_4800_2 = 0x001f;
    private static final int CH34X_9600_1 = 0xb202;
    private static final int CH34X_9600_2 = 0x0013;
    private static final int CH34X_19200_1 = 0xd902;
    private static final int CH34X_19200_2 = 0x000d;
    private static final int CH34X_38400_1 = 0x6403;
    private static final int CH34X_38400_2 = 0x000a;
    private static final int CH34X_115200_1 = 0xcc03;
    private static final int CH34X_115200_2 = 0x0008;


    private UsbInterface mInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;
    private UsbRequest requestIN;

    private boolean dtr = false;
    private boolean rts = false;


    public CH34xSerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        super(device, connection);
    }

    public CH34xSerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface)
    {
        super(device, connection);
        mInterface = device.getInterface(iface >= 0 ? iface : 0);
    }

    @Override
    public boolean open()
    {
        if(connection.claimInterface(mInterface, true))
        {
            Log.i(CLASS_ID, "Interface succesfully claimed");
        }else
        {
            Log.i(CLASS_ID, "Interface could not be claimed");
            return false;
        }

        // Assign endpoints
        int numberEndpoints = mInterface.getEndpointCount();
        for(int i=0;i<=numberEndpoints-1;i++)
        {
            UsbEndpoint endpoint = mInterface.getEndpoint(i);
            if(endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && endpoint.getDirection() == UsbConstants.USB_DIR_IN)
            {
                inEndpoint = endpoint;
            }else if(endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && endpoint.getDirection() == UsbConstants.USB_DIR_OUT)
            {
                outEndpoint = endpoint;
            }
        }

        // Default Setup
        if(init() == 0)
        {
            setBaudRate(DEFAULT_BAUDRATE);
            // Initialize UsbRequest
            requestIN = new UsbRequest();
            requestIN.initialize(connection, inEndpoint);

            // Restart the working thread if it has been killed before and  get and claim interface
            restartWorkingThread();
            restartWriteThread();

            // Pass references to the threads
            setThreadsParams(requestIN, outEndpoint);

            return true;
        }else
        {
            return false;
        }
    }

    @Override
    public void close()
    {
        killWorkingThread();
        killWriteThread();
        connection.releaseInterface(mInterface);
    }

    @Override
    public void setBaudRate(int baudRate)
    {
        if(baudRate <= 2400)
        {
            int ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x1312, CH34X_2400_1, null);
            if(ret < 0)
            {
                Log.i(CLASS_ID, "Error setting baudRate");
            }else
            {
                ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x0f2c, CH34X_2400_2, null);
                if(ret < 0)
                    Log.i(CLASS_ID, "Error setting baudRate");
                else
                    Log.i(CLASS_ID, "BaudRate set correctly");
            }

        }else if(baudRate > 2400 && baudRate <= 4800)
        {
            int ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x1312, CH34X_4800_1, null);
            if(ret < 0)
            {
                Log.i(CLASS_ID, "Error setting baudRate");
            }else
            {
                ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x0f2c, CH34X_4800_2, null);
                if(ret < 0)
                    Log.i(CLASS_ID, "Error setting baudRate");
                else
                    Log.i(CLASS_ID, "BaudRate set correctly");
            }
        }else if(baudRate > 4800 && baudRate <= 9600)
        {
            int ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x1312, CH34X_9600_1, null);
            if(ret < 0)
            {
                Log.i(CLASS_ID, "Error setting baudRate");
            }else
            {
                ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x0f2c, CH34X_9600_2, null);
                if(ret < 0)
                    Log.i(CLASS_ID, "Error setting baudRate");
                else
                    Log.i(CLASS_ID, "BaudRate set correctly");
            }
        }else if(baudRate > 9600 && baudRate <= 19200)
        {
            int ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x1312, CH34X_19200_1, null);
            if(ret < 0)
            {
                Log.i(CLASS_ID, "Error setting baudRate");
            }else
            {
                ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x0f2c, CH34X_19200_2, null);
                if(ret < 0)
                    Log.i(CLASS_ID, "Error setting baudRate");
                else
                    Log.i(CLASS_ID, "BaudRate set correctly");
            }
        }else if(baudRate > 19200 && baudRate <= 38400)
        {
            int ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x1312, CH34X_38400_1, null);
            if(ret < 0)
            {
                Log.i(CLASS_ID, "Error setting baudRate");
            }else
            {
                ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x0f2c, CH34X_38400_2, null);
                if(ret < 0)
                    Log.i(CLASS_ID, "Error setting baudRate");
                else
                    Log.i(CLASS_ID, "BaudRate set correctly");
            }
        }else if(baudRate > 38400)
        {
            int ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x1312, CH34X_115200_1, null);
            if(ret < 0)
            {
                Log.i(CLASS_ID, "Error setting baudRate");
            }else
            {
                ret = setControlCommandOut(CH341_REQ_WRITE_REG, 0x0f2c, CH34X_115200_2, null);
                if(ret < 0)
                    Log.i(CLASS_ID, "Error setting baudRate");
                else
                    Log.i(CLASS_ID, "BaudRate set correctly");
            }
        }
    }

    @Override
    public void setDataBits(int dataBits)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setStopBits(int stopBits)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setParity(int parity)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFlowControl(int flowControl)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRTS(boolean state)
    {
        //TODO
    }

    @Override
    public void setDTR(boolean state)
    {
        //TODO
    }

    @Override
    public void getCTS(UsbCTSCallback ctsCallback)
    {
        //TODO
    }

    @Override
    public void getDSR(UsbDSRCallback dsrCallback)
    {
        //TODO
    }

    @Override
    public void getBreak(UsbBreakCallback breakCallback)
    {
        //TODO
    }

    @Override
    public void getFrame(UsbFrameCallback frameCallback)
    {
        //TODO
    }

    @Override
    public void getOverrun(UsbOverrunCallback overrunCallback)
    {
        //TODO
    }

    @Override
    public void getParity(UsbParityCallback parityCallback)
    {
        //TODO
    }

    private int init()
    {
        if(checkState("init #1", 0x5f, 0, new int[]{-1 /* 0x27, 0x30 */, 0x00}) == -1)
        {
            return -1;
        }

        if(setControlCommandOut(0xa1, 0, 0, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #2");
            return -1;
        }

        setBaudRate(DEFAULT_BAUDRATE);

        if(checkState("init #4", 0x95, 0x2518, new int[]{-1 /* 0x56, c3*/, 0x00}) == -1)
            return -1;

        if(setControlCommandOut(0x9a, 0x2518, 0x0050, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #5");
            return -1;
        }


        if(checkState("init #6", 0x95, 0x0706, new int[]{0xff, 0xee}) == -1)
            return -1;

        if(setControlCommandOut(0xa1, 0x501f, 0xd90a, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #7");
            return -1;
        }

        setBaudRate(DEFAULT_BAUDRATE);

        if(writeHandshakeByte() == -1)
            return -1;

        if(checkState("init #10", 0x95, 0x0706, new int[]{-1/* 0x9f, 0xff*/, 0xee}) == -1)
            return -1;
        else
            return 0;
    }

    private int checkState(String msg, int request, int value, int[] expected)
    {
        byte[] buffer = new byte[expected.length];
        int ret = setControlCommandIn(request, value, 0, buffer);

        if (ret != expected.length)
        {
            Log.i(CLASS_ID, ("Expected " + expected.length + " bytes, but get " + ret + " [" + msg + "]"));
            return -1;
        }else
        {
            for (int i = 0; i < expected.length; i++)
            {
                if (expected[i] == -1)
                {
                    continue;
                }

                int current = buffer[i] & 0xff;
                if (expected[i] != current)
                {
                    Log.i(CLASS_ID, "Expected 0x" + Integer.toHexString(expected[i]) + " bytes, but get 0x" + Integer.toHexString(current) + " [" + msg + "]");
                    return -1;
                }
            }
            return 0;
        }
    }

    private int writeHandshakeByte()
    {
        if(setControlCommandOut(0xa4, ~((dtr ? 1 << 5 : 0) | (rts ? 1 << 6 : 0)), 0, null) < 0)
        {
            Log.i(CLASS_ID, "Faild to set handshake byte");
            return -1;
        }else if(setControlCommandOut(0xa4, ~((dtr ? 1 << 5 : 0) | (rts ? 1 << 6 : 0)), 0, null) > 0)
        {
            return 0;
        }
        return 0;
    }

    private int setControlCommandOut(int request, int value, int index, byte[] data)
    {
        int dataLength = 0;
        if(data != null)
        {
            dataLength = data.length;
        }
        int response = connection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request, value, index, data, dataLength, USB_TIMEOUT);
        Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
        return response;
    }

    private int setControlCommandIn(int request, int value, int index, byte[] data)
    {
        int dataLength = 0;
        if(data != null)
        {
            dataLength = data.length;
        }
        int response = connection.controlTransfer(REQTYPE_HOST_FROM_DEVICE, request, value, index, data, dataLength, USB_TIMEOUT);
        Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
        return response;
    }

}
