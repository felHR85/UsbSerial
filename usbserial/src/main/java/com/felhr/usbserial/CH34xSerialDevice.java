/*
 * Based in the CH340x driver made by Andreas Butti (https://github.com/mik3y/usb-serial-for-android/blob/master/usbSerialForAndroid/src/main/java/com/hoho/android/usbserial/driver/Ch34xSerialDriver.java)
 * Thanks to Paul Alcock for provide me with one of those Arduino nano clones!!!
 * Also thanks to Lex Wernars for send me a CH340 that didnt work with the former version of this code!!
 * */

package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.felhr.utils.SafeUsbRequest;

public class CH34xSerialDevice extends UsbSerialDevice
{
    private static final String CLASS_ID = CH34xSerialDevice.class.getSimpleName();

    private static final int DEFAULT_BAUDRATE = 9600;

    private static final int REQTYPE_HOST_FROM_DEVICE = UsbConstants.USB_TYPE_VENDOR | UsbConstants.USB_DIR_IN;
    private static final int REQTYPE_HOST_TO_DEVICE = 0x40;

    private static final int CH341_REQ_WRITE_REG = 0x9A;
    private static final int CH341_REQ_READ_REG = 0x95;
    private static final int CH341_REG_BREAK1 = 0x05;
    private static final int CH341_REG_BREAK2 = 0x18;
    private static final int CH341_NBREAK_BITS_REG1 = 0x01;
    private static final int CH341_NBREAK_BITS_REG2 = 0x40;

    // Baud rates values
    private static final int CH34X_300_1312 = 0xd980;
    private static final int CH34X_300_0f2c = 0xeb;

    private static final int CH34X_600_1312 = 0x6481;
    private static final int CH34X_600_0f2c = 0x76;

    private static final int CH34X_1200_1312 = 0xb281;
    private static final int CH34X_1200_0f2c = 0x3b;

    private static final int CH34X_2400_1312 = 0xd981;
    private static final int CH34X_2400_0f2c = 0x1e;

    private static final int CH34X_4800_1312 = 0x6482;
    private static final int CH34X_4800_0f2c = 0x0f;

    private static final int CH34X_9600_1312 = 0xb282;
    private static final int CH34X_9600_0f2c = 0x08;

    private static final int CH34X_19200_1312 = 0xd982;
    private static final int CH34X_19200_0f2c_rest = 0x07;

    private static final int CH34X_38400_1312 = 0x6483;

    private static final int CH34X_57600_1312 = 0x9883;

    private static final int CH34X_115200_1312 = 0xcc83;

    private static final int CH34X_230400_1312 = 0xe683;

    private static final int CH34X_460800_1312 = 0xf383;

    private static final int CH34X_921600_1312 = 0xf387;

    // Parity values
    private static final int CH34X_PARITY_NONE = 0xc3;
    private static final int CH34X_PARITY_ODD = 0xcb;
    private static final int CH34X_PARITY_EVEN = 0xdb;
    private static final int CH34X_PARITY_MARK = 0xeb;
    private static final int CH34X_PARITY_SPACE = 0xfb;

    //Flow control values
    private static final int CH34X_FLOW_CONTROL_NONE = 0x0000;
    private static final int CH34X_FLOW_CONTROL_RTS_CTS = 0x0101;
    private static final int CH34X_FLOW_CONTROL_DSR_DTR = 0x0202;
    // XON/XOFF doesnt appear to be supported directly from hardware


    private UsbInterface mInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;

    private FlowControlThread flowControlThread;
    private UsbCTSCallback ctsCallback;
    private UsbDSRCallback dsrCallback;
    private boolean rtsCtsEnabled;
    private boolean dtrDsrEnabled;
    private boolean dtr = false;
    private boolean rts = false;
    private boolean ctsState = false;
    private boolean dsrState = false;


    public CH34xSerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        super(device, connection);
    }

    public CH34xSerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface)
    {
        super(device, connection);
        rtsCtsEnabled = false;
        dtrDsrEnabled = false;
        mInterface = device.getInterface(iface >= 0 ? iface : 0);
    }

    @Override
    public boolean open()
    {
        boolean ret = openCH34X();
        if(ret)
        {
            // Initialize UsbRequest
            UsbRequest requestIN = new SafeUsbRequest();
            requestIN.initialize(connection, inEndpoint);

            // Restart the working thread if it has been killed before and  get and claim interface
            restartWorkingThread();
            restartWriteThread();

            // Create Flow control thread but it will only be started if necessary
            createFlowControlThread();

            // Pass references to the threads
            setThreadsParams(requestIN, outEndpoint);

            asyncMode = true;
            isOpen = true;

            return true;
        }else
        {
            isOpen = false;
            return false;
        }
    }

    @Override
    public void close()
    {
        killWorkingThread();
        killWriteThread();
        stopFlowControlThread();
        connection.releaseInterface(mInterface);
        isOpen = false;
    }

    @Override
    public boolean syncOpen()
    {
        boolean ret = openCH34X();
        if(ret)
        {
            // Create Flow control thread but it will only be started if necessary
            createFlowControlThread();
            setSyncParams(inEndpoint, outEndpoint);
            asyncMode = false;
            isOpen = true;

            // Init Streams
            inputStream = new SerialInputStream(this);
            outputStream = new SerialOutputStream(this);

            return true;
        }else
        {
            isOpen = false;
            return false;
        }
    }

    @Override
    public void syncClose()
    {
        stopFlowControlThread();
        connection.releaseInterface(mInterface);
        isOpen = false;
    }

    @Override
    public void setBaudRate(int baudRate)
    {
        if(baudRate <= 300)
        {
            int ret = setBaudRate(CH34X_300_1312, CH34X_300_0f2c); //300
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 300  && baudRate <= 600)
        {
            int ret = setBaudRate(CH34X_600_1312, CH34X_600_0f2c); //600
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");

        }else if(baudRate > 600 && baudRate <= 1200)
        {
            int ret = setBaudRate(CH34X_1200_1312, CH34X_1200_0f2c); //1200
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 1200 && baudRate <=2400)
        {
            int ret = setBaudRate(CH34X_2400_1312, CH34X_2400_0f2c); //2400
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 2400 && baudRate <= 4800)
        {
            int ret = setBaudRate(CH34X_4800_1312, CH34X_4800_0f2c); //4800
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 4800 && baudRate <= 9600)
        {
            int ret = setBaudRate(CH34X_9600_1312, CH34X_9600_0f2c); //9600
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 9600 && baudRate <= 19200)
        {
            int ret = setBaudRate(CH34X_19200_1312, CH34X_19200_0f2c_rest); //19200
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 19200 && baudRate <= 38400)
        {
            int ret = setBaudRate(CH34X_38400_1312, CH34X_19200_0f2c_rest); //38400
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 38400 && baudRate <= 57600)
        {
            int ret = setBaudRate(CH34X_57600_1312, CH34X_19200_0f2c_rest); //57600
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 57600 && baudRate <= 115200) //115200
        {
            int ret = setBaudRate(CH34X_115200_1312, CH34X_19200_0f2c_rest);
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 115200 && baudRate <= 230400) //230400
        {
            int ret = setBaudRate(CH34X_230400_1312, CH34X_19200_0f2c_rest);
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 230400 && baudRate <= 460800) //460800
        {
            int ret = setBaudRate(CH34X_460800_1312, CH34X_19200_0f2c_rest);
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
        }else if(baudRate > 460800 && baudRate <= 921600)
        {
            int ret = setBaudRate(CH34X_921600_1312, CH34X_19200_0f2c_rest);
            if(ret == -1)
                Log.i(CLASS_ID, "SetBaudRate failed!");
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
    public void setBreak(boolean state)
    {
        //TODO Auto-generated method stub
    }

    @Override
    public void setParity(int parity)
    {
        switch(parity)
        {
            case UsbSerialInterface.PARITY_NONE:
                setCh340xParity(CH34X_PARITY_NONE);
                break;
            case UsbSerialInterface.PARITY_ODD:
                setCh340xParity(CH34X_PARITY_ODD);
                break;
            case UsbSerialInterface.PARITY_EVEN:
                setCh340xParity(CH34X_PARITY_EVEN);
                break;
            case UsbSerialInterface.PARITY_MARK:
                setCh340xParity(CH34X_PARITY_MARK);
                break;
            case UsbSerialInterface.PARITY_SPACE:
                setCh340xParity(CH34X_PARITY_SPACE);
                break;
            default:
                break;
        }
    }

    @Override
    public void setFlowControl(int flowControl)
    {
        switch(flowControl)
        {
            case UsbSerialInterface.FLOW_CONTROL_OFF:
                rtsCtsEnabled = false;
                dtrDsrEnabled = false;
                setCh340xFlow(CH34X_FLOW_CONTROL_NONE);
                break;
            case UsbSerialInterface.FLOW_CONTROL_RTS_CTS:
                rtsCtsEnabled = true;
                dtrDsrEnabled = false;
                setCh340xFlow(CH34X_FLOW_CONTROL_RTS_CTS);
                ctsState = checkCTS();
                startFlowControlThread();
                break;
            case UsbSerialInterface.FLOW_CONTROL_DSR_DTR:
                rtsCtsEnabled = false;
                dtrDsrEnabled = true;
                setCh340xFlow(CH34X_FLOW_CONTROL_DSR_DTR);
                dsrState = checkDSR();
                startFlowControlThread();
                break;
            default:
                break;
        }
    }

    @Override
    public void setRTS(boolean state)
    {
        rts = state;
        writeHandshakeByte();
    }

    @Override
    public void setDTR(boolean state)
    {
        dtr = state;
        writeHandshakeByte();
    }

    @Override
    public void getCTS(UsbCTSCallback ctsCallback)
    {
        this.ctsCallback = ctsCallback;
    }

    @Override
    public void getDSR(UsbDSRCallback dsrCallback)
    {
        this.dsrCallback = dsrCallback;
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

    private boolean openCH34X()
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

        return init() == 0;
    }

    private int init()
    {
        /*
            Init the device at 9600 bauds
         */

        if(setControlCommandOut(0xa1, 0xc29c, 0xb2b9, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #1");
            return -1;
        }

        if(setControlCommandOut(0xa4, 0xdf, 0, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #2");
            return -1;
        }

        if(setControlCommandOut(0xa4, 0x9f, 0, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #3");
            return -1;
        }

        if(checkState("init #4", 0x95, 0x0706, new int[]{0x9f, 0xee}) == -1)
            return -1;

        if(setControlCommandOut(0x9a, 0x2727, 0x0000, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #5");
            return -1;
        }

        if(setControlCommandOut(0x9a, 0x1312, 0xb282, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #6");
            return -1;
        }

        if(setControlCommandOut(0x9a, 0x0f2c, 0x0008, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #7");
            return -1;
        }

        if(setControlCommandOut(0x9a, 0x2518, 0x00c3, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #8");
            return -1;
        }

        if(checkState("init #9", 0x95, 0x0706, new int[]{0x9f, 0xee}) == -1)
            return -1;

        if(setControlCommandOut(0x9a, 0x2727, 0x0000, null) < 0)
        {
            Log.i(CLASS_ID, "init failed! #10");
            return -1;
        }

        return 0;
    }

    private int setBaudRate(int index1312, int index0f2c)
    {
        if(setControlCommandOut(CH341_REQ_WRITE_REG, 0x1312, index1312, null) < 0)
            return -1;
        if(setControlCommandOut(CH341_REQ_WRITE_REG, 0x0f2c, index0f2c, null) < 0)
            return -1;
        if(checkState("set_baud_rate", 0x95, 0x0706, new int[]{0x9f, 0xee}) == -1)
            return -1;
        if(setControlCommandOut(CH341_REQ_WRITE_REG, 0x2727, 0, null) < 0)
            return -1;
        return 0;
    }

    private int setCh340xParity(int indexParity)
    {
        if(setControlCommandOut(CH341_REQ_WRITE_REG, 0x2518, indexParity, null) < 0)
            return -1;
        if(checkState("set_parity", 0x95, 0x0706, new int[]{0x9f, 0xee}) == -1)
            return -1;
        if(setControlCommandOut(CH341_REQ_WRITE_REG, 0x2727, 0, null) < 0)
            return -1;
        return 0;
    }

    private int setCh340xFlow(int flowControl)
    {
        if(checkState("set_flow_control", 0x95, 0x0706, new int[]{0x9f, 0xee}) == -1)
            return -1;
        if(setControlCommandOut(CH341_REQ_WRITE_REG, 0x2727, flowControl, null) == -1)
            return -1;
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
            return 0;
        }
    }

    private boolean checkCTS()
    {
        byte[] buffer = new byte[2];
        int ret = setControlCommandIn(CH341_REQ_READ_REG, 0x0706, 0, buffer);

        if(ret != 2)
        {
            Log.i(CLASS_ID, ("Expected " + "2" + " bytes, but get " + ret));
            return false;
        }

        if((buffer[0] & 0x01) == 0x00) //CTS ON
        {
            return true;
        }else // CTS OFF
        {
            return false;
        }
    }

    private boolean checkDSR()
    {
        byte[] buffer = new byte[2];
        int ret = setControlCommandIn(CH341_REQ_READ_REG, 0x0706, 0, buffer);

        if(ret != 2)
        {
            Log.i(CLASS_ID, ("Expected " + "2" + " bytes, but get " + ret));
            return false;
        }

        if((buffer[0] & 0x02) == 0x00) //DSR ON
        {
            return true;
        }else // DSR OFF
        {
            return false;
        }
    }

    private int writeHandshakeByte()
    {
        if(setControlCommandOut(0xa4, ~((dtr ? 1 << 5 : 0) | (rts ? 1 << 6 : 0)), 0, null) < 0)
        {
            Log.i(CLASS_ID, "Failed to set handshake byte");
            return -1;
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

    private void createFlowControlThread()
    {
        flowControlThread = new FlowControlThread();
    }

    private void startFlowControlThread()
    {
        if(!flowControlThread.isAlive())
            flowControlThread.start();
    }

    private void stopFlowControlThread()
    {
        if(flowControlThread != null)
        {
            flowControlThread.stopThread();
            flowControlThread = null;
        }
    }

    private class FlowControlThread extends AbstractWorkerThread
    {
        private final long time = 100; // 100ms

        @Override
        public void doRun()
        {
            if(!firstTime)
            {
                // Check CTS status
                if(rtsCtsEnabled)
                {
                    boolean cts = pollForCTS();
                    if(ctsState != cts)
                    {
                        ctsState = !ctsState;
                        if (ctsCallback != null)
                            ctsCallback.onCTSChanged(ctsState);
                    }
                }

                // Check DSR status
                if(dtrDsrEnabled)
                {
                    boolean dsr = pollForDSR();
                    if(dsrState != dsr)
                    {
                        dsrState = !dsrState;
                        if (dsrCallback != null)
                            dsrCallback.onDSRChanged(dsrState);
                    }
                }
            }else
            {
                if(rtsCtsEnabled && ctsCallback != null)
                    ctsCallback.onCTSChanged(ctsState);

                if(dtrDsrEnabled && dsrCallback != null)
                    dsrCallback.onDSRChanged(dsrState);

                firstTime = false;
            }
        }

        public boolean pollForCTS()
        {
            synchronized(this)
            {
                try
                {
                    wait(time);
                } catch(InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            return checkCTS();
        }

        public boolean pollForDSR()
        {
            synchronized(this)
            {
                try
                {
                    wait(time);
                } catch(InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            return checkDSR();
        }
    }
}
