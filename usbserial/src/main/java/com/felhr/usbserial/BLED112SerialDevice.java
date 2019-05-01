package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

@Deprecated
public class BLED112SerialDevice extends UsbSerialDevice
{
    private static final String CLASS_ID = BLED112SerialDevice.class.getSimpleName();

    private static final int BLED112_REQTYPE_HOST2DEVICE = 0x21;
    private static final int BLED112_REQTYPE_DEVICE2HOST = 0xA1;

    private static final int BLED112_SET_LINE_CODING = 0x20;
    private static final int BLED112_GET_LINE_CODING = 0x21;
    private static final int BLED112_SET_CONTROL_LINE_STATE = 0x22;

    /***
     *  Default Serial Configuration
     *  Baud rate: 115200
     *  Data bits: 8
     *  Stop bits: 1
     *  Parity: None
     *  Flow Control: Off
     */
    private static final byte[] BLED112_DEFAULT_LINE_CODING = new byte[] {
            (byte) 0x00, // Offset 0:4 dwDTERate
            (byte) 0x01,
            (byte) 0xC2,
            (byte) 0x00,
            (byte) 0x00, // Offset 5 bCharFormat (1 Stop bit)
            (byte) 0x00, // bParityType (None)
            (byte) 0x08  // bDataBits (8)
    };

    private static final int BLED112_DEFAULT_CONTROL_LINE = 0x0003;
    private static final int BLED112_DISCONNECT_CONTROL_LINE = 0x0002;

    private final UsbInterface mInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;

    @Deprecated
    public BLED112SerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        super(device, connection);
        mInterface = device.getInterface(1); // BLED112 Interface 0: Communications | Interface 1: CDC Data
    }

    @Override
    public boolean open()
    {
        // Restart the working thread if it has been killed before and  get and claim interface
        restartWorkingThread();
        restartWriteThread();

        if(connection.claimInterface(mInterface, true))
        {
            Log.i(CLASS_ID, "Interface succesfully claimed");
        }else
        {
            Log.i(CLASS_ID, "Interface could not be claimed");
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
            }else
            {
                outEndpoint = endpoint;
            }
        }

        // Default Setup
        setControlCommand(BLED112_SET_LINE_CODING, 0, BLED112_DEFAULT_LINE_CODING);
        setControlCommand(BLED112_SET_CONTROL_LINE_STATE, BLED112_DEFAULT_CONTROL_LINE, null);

        // Initialize UsbRequest
        UsbRequest requestIN = new UsbRequest();
        requestIN.initialize(connection, inEndpoint);

        // Pass references to the threads
        setThreadsParams(requestIN, outEndpoint);

        return true;
    }

    @Override
    public void close()
    {
        setControlCommand(BLED112_SET_CONTROL_LINE_STATE, BLED112_DISCONNECT_CONTROL_LINE , null);
        killWorkingThread();
        killWriteThread();
        connection.releaseInterface(mInterface);
    }

    @Override
    public boolean syncOpen()
    {
        return false;
    }

    @Override
    public void syncClose()
    {

    }

    @Override
    public void setBaudRate(int baudRate)
    {
        byte[] data = getLineCoding();

        data[3] = (byte) (baudRate & 0xff);
        data[2] = (byte) (baudRate >> 8 & 0xff);
        data[1] = (byte) (baudRate >> 16 & 0xff);
        data[0] = (byte) (baudRate >> 24 & 0xff);

        setControlCommand(BLED112_SET_LINE_CODING, 0, data);
    }

    @Override
    public void setDataBits(int dataBits)
    {
        byte[] data = getLineCoding();
        switch(dataBits)
        {
            case UsbSerialInterface.DATA_BITS_5:
                data[6] = 0x05;
                break;
            case UsbSerialInterface.DATA_BITS_6:
                data[6] = 0x06;
                break;
            case UsbSerialInterface.DATA_BITS_7:
                data[6] = 0x07;
                break;
            case UsbSerialInterface.DATA_BITS_8:
                data[6] = 0x08;
                break;
        }

        setControlCommand(BLED112_SET_LINE_CODING, 0, data);

    }

    @Override
    public void setStopBits(int stopBits)
    {
        byte[] data = getLineCoding();
        switch(stopBits)
        {
            case UsbSerialInterface.STOP_BITS_1:
                data[4] = 0x00;
                break;
            case UsbSerialInterface.STOP_BITS_15:
                data[4] = 0x01;
                break;
            case UsbSerialInterface.STOP_BITS_2:
                data[4] = 0x02;
                break;
        }

        setControlCommand(BLED112_SET_LINE_CODING, 0, data);


    }

    @Override
    public void setParity(int parity)
    {
        byte[] data = getLineCoding();
        switch(parity)
        {
            case UsbSerialInterface.PARITY_NONE:
                data[5] = 0x00;
                break;
            case UsbSerialInterface.PARITY_ODD:
                data[5] = 0x01;
                break;
            case UsbSerialInterface.PARITY_EVEN:
                data[5] = 0x02;
                break;
            case UsbSerialInterface.PARITY_MARK:
                data[5] = 0x03;
                break;
            case UsbSerialInterface.PARITY_SPACE:
                data[5] = 0x04;
                break;
        }

        setControlCommand(BLED112_SET_LINE_CODING, 0, data);

    }

    @Override
    public void setBreak(boolean state)
    {
        //TODO
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
    public void setFlowControl(int flowControl)
    {
        // TODO Auto-generated method stub

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

    private int setControlCommand(int request, int value, byte[] data)
    {
        int dataLength = 0;
        if(data != null)
        {
            dataLength = data.length;
        }
        int response = connection.controlTransfer(BLED112_REQTYPE_HOST2DEVICE, request, value, 0, data, dataLength, USB_TIMEOUT);
        Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
        return response;
    }

    private byte[] getLineCoding()
    {
        byte[] data = new byte[7];
        int response = connection.controlTransfer(BLED112_REQTYPE_DEVICE2HOST, BLED112_GET_LINE_CODING, 0, 0, data, data.length, USB_TIMEOUT);
        Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
        return data;
    }


}
