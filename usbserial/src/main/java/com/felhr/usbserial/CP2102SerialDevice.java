package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.felhr.utils.SafeUsbRequest;

public class CP2102SerialDevice extends UsbSerialDevice
{
    private static final String CLASS_ID = CP2102SerialDevice.class.getSimpleName();

    private static final int CP210x_PURGE = 0x12;
    private static final int CP210x_IFC_ENABLE = 0x00;
    private static final int CP210x_SET_BAUDDIV = 0x01;
    private static final int CP210x_SET_LINE_CTL = 0x03;
    private static final int CP210x_GET_LINE_CTL = 0x04;
    private static final int CP210x_SET_MHS = 0x07;
    private static final int CP210x_SET_BAUDRATE = 0x1E;
    private static final int CP210x_SET_FLOW = 0x13;
    private static final int CP210x_SET_XON = 0x09;
    private static final int CP210x_SET_XOFF = 0x0A;
    private static final int CP210x_SET_CHARS = 0x19;
    private static final int CP210x_GET_MDMSTS = 0x08;
    private static final int CP210x_GET_COMM_STATUS = 0x10;

    private static final int CP210x_REQTYPE_HOST2DEVICE = 0x41;
    private static final int CP210x_REQTYPE_DEVICE2HOST = 0xC1;

    private static final int CP210x_MHS_RTS_ON = 0x202;
    private static final int CP210x_MHS_RTS_OFF = 0x200;
    private static final int CP210x_MHS_DTR_ON = 0x101;
    private static final int CP210x_MHS_DTR_OFF = 0x100;

    private static final int CP210x_PURGE_ALL = 0x000f;

    /***
     *  Default Serial Configuration
     *  Baud rate: 9600
     *  Data bits: 8
     *  Stop bits: 1
     *  Parity: None
     *  Flow Control: Off
     */
    private static final int CP210x_UART_ENABLE = 0x0001;
    private static final int CP210x_UART_DISABLE = 0x0000;
    private static final int CP210x_LINE_CTL_DEFAULT = 0x0800;
    private static final int CP210x_MHS_DEFAULT = 0x0000;
    private static final int CP210x_MHS_DTR = 0x0001;
    private static final int CP210x_MHS_RTS = 0x0010;
    private static final int CP210x_MHS_ALL = 0x0011;
    private static final int CP210x_XON = 0x0000;
    private static final int CP210x_XOFF = 0x0000;
    private static final int DEFAULT_BAUDRATE = 9600;

    /**
     * Flow control variables
     */
    private boolean rtsCtsEnabled;
    private boolean dtrDsrEnabled;
    private boolean ctsState;
    private boolean dsrState;

    private UsbCTSCallback ctsCallback;
    private UsbDSRCallback dsrCallback;

    private final UsbInterface mInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;

    private FlowControlThread flowControlThread;

    // COMM_STATUS callbacks
    private UsbSerialInterface.UsbParityCallback parityCallback;
    private UsbSerialInterface.UsbBreakCallback breakCallback;
    private UsbSerialInterface.UsbFrameCallback frameCallback;
    private UsbSerialInterface.UsbOverrunCallback overrunCallback;

    public CP2102SerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        this(device, connection, -1);
    }

    public CP2102SerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface)
    {
        super(device, connection);
        rtsCtsEnabled = false;
        dtrDsrEnabled = false;
        ctsState = true;
        dsrState = true;
        mInterface = device.getInterface(iface >= 0 ? iface : 0);
    }

    @Override
    public boolean open()
    {
        boolean ret = openCP2102();

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
        setControlCommand(CP210x_PURGE, CP210x_PURGE_ALL, null);
        setControlCommand(CP210x_IFC_ENABLE, CP210x_UART_DISABLE, null);
        killWorkingThread();
        killWriteThread();
        stopFlowControlThread();
        connection.releaseInterface(mInterface);
        isOpen = false;
    }

    @Override
    public boolean syncOpen()
    {
        boolean ret = openCP2102();
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
        setControlCommand(CP210x_PURGE, CP210x_PURGE_ALL, null);
        setControlCommand(CP210x_IFC_ENABLE, CP210x_UART_DISABLE, null);
        stopFlowControlThread();
        connection.releaseInterface(mInterface);
        isOpen = false;
    }

    @Override
    public void setBaudRate(int baudRate)
    {
        byte[] data = new byte[] {
                (byte) (baudRate & 0xff),
                (byte) (baudRate >> 8 & 0xff),
                (byte) (baudRate >> 16 & 0xff),
                (byte) (baudRate >> 24 & 0xff)
        };
        setControlCommand(CP210x_SET_BAUDRATE, 0, data);
    }

    @Override
    public void setDataBits(int dataBits)
    {
        short wValue = getCTL();
        wValue &= ~0x0F00;
        switch(dataBits)
        {
            case UsbSerialInterface.DATA_BITS_5:
                wValue |= 0x0500;
                break;
            case UsbSerialInterface.DATA_BITS_6:
                wValue |= 0x0600;
                break;
            case UsbSerialInterface.DATA_BITS_7:
                wValue |= 0x0700;
                break;
            case UsbSerialInterface.DATA_BITS_8:
                wValue |= 0x0800;
                break;
            default:
                return;
        }
        setControlCommand(CP210x_SET_LINE_CTL, wValue, null);
    }

    @Override
    public void setStopBits(int stopBits)
    {
        short wValue = getCTL();
        wValue &= ~0x0003;
        switch(stopBits)
        {
            case UsbSerialInterface.STOP_BITS_1:
                wValue |= 0;
                break;
            case UsbSerialInterface.STOP_BITS_15:
                wValue |= 1;
                break;
            case UsbSerialInterface.STOP_BITS_2:
                wValue |= 2;
                break;
            default:
                return;
        }
        setControlCommand(CP210x_SET_LINE_CTL, wValue, null);
    }

    @Override
    public void setParity(int parity)
    {
        short wValue = getCTL();
        wValue &= ~0x00F0;
        switch(parity)
        {
            case UsbSerialInterface.PARITY_NONE:
                wValue |= 0x0000;
                break;
            case UsbSerialInterface.PARITY_ODD:
                wValue |= 0x0010;
                break;
            case UsbSerialInterface.PARITY_EVEN:
                wValue |= 0x0020;
                break;
            case UsbSerialInterface.PARITY_MARK:
                wValue |= 0x0030;
                break;
            case UsbSerialInterface.PARITY_SPACE:
                wValue |= 0x0040;
                break;
            default:
                return;
        }
        setControlCommand(CP210x_SET_LINE_CTL, wValue, null);
    }

    @Override
    public void setFlowControl(int flowControl)
    {
        switch(flowControl)
        {
            case UsbSerialInterface.FLOW_CONTROL_OFF:
                byte[] dataOff = new byte[]{
                        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x00
                };
                rtsCtsEnabled = false;
                dtrDsrEnabled = false;
                setControlCommand(CP210x_SET_FLOW, 0, dataOff);
                break;
            case UsbSerialInterface.FLOW_CONTROL_RTS_CTS:
                byte[] dataRTSCTS = new byte[]{
                        (byte) 0x09, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x00
                };
                rtsCtsEnabled = true;
                dtrDsrEnabled = false;
                setControlCommand(CP210x_SET_FLOW, 0, dataRTSCTS);
                setControlCommand(CP210x_SET_MHS, CP210x_MHS_RTS_ON, null);
                byte[] commStatusCTS = getCommStatus();
                ctsState = (commStatusCTS[4] & 0x01) == 0x00;
                startFlowControlThread();
                break;
            case UsbSerialInterface.FLOW_CONTROL_DSR_DTR:
                byte[] dataDSRDTR = new byte[]{
                        (byte) 0x11, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x00
                };
                dtrDsrEnabled = true;
                rtsCtsEnabled = false;
                setControlCommand(CP210x_SET_FLOW, 0, dataDSRDTR);
                setControlCommand(CP210x_SET_MHS, CP210x_MHS_DTR_ON, null);
                byte[] commStatusDSR = getCommStatus();
                dsrState = (commStatusDSR[4] & 0x02) == 0x00;
                startFlowControlThread();
                break;
            case UsbSerialInterface.FLOW_CONTROL_XON_XOFF:
                byte[] dataXONXOFF = new byte[]{
                        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x00
                };

                byte[] dataChars = new byte[]{
                        (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x11, (byte) 0x13
                };
                setControlCommand(CP210x_SET_CHARS, 0, dataChars);
                setControlCommand(CP210x_SET_FLOW, 0, dataXONXOFF);
                break;
            default:
                return;
        }
    }

    @Override
    public void setBreak(boolean state)
    {
        //TODO Auto-generated method stub
    }

    @Override
    public void setRTS(boolean state)
    {
        if(state)
        {
            setControlCommand(CP210x_SET_MHS, CP210x_MHS_RTS_ON, null);
        }else
        {
            setControlCommand(CP210x_SET_MHS, CP210x_MHS_RTS_OFF, null);
        }
    }

    @Override
    public void setDTR(boolean state)
    {
        if(state)
        {
            setControlCommand(CP210x_SET_MHS, CP210x_MHS_DTR_ON, null);
        }else
        {
            setControlCommand(CP210x_SET_MHS, CP210x_MHS_DTR_OFF, null);
        }
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
        this.breakCallback = breakCallback;
    }

    @Override
    public void getFrame(UsbFrameCallback frameCallback)
    {
        this.frameCallback = frameCallback;
    }

    @Override
    public void getOverrun(UsbOverrunCallback overrunCallback)
    {
        this.overrunCallback = overrunCallback;
    }

    @Override
    public void getParity(UsbParityCallback parityCallback)
    {
        this.parityCallback = parityCallback;
        startFlowControlThread();
    }

    /*
        Thread to check every X time if flow signals CTS or DSR have been raised
    */
    private class FlowControlThread extends AbstractWorkerThread
    {
        private final long time = 40; // 40ms

        @Override
        public void doRun()
        {
            if(!firstTime) // Only execute the callback when the status change
            {
                byte[] modemState = pollLines();
                byte[] commStatus = getCommStatus();

                // Check CTS status
                if(rtsCtsEnabled)
                {
                    if(ctsState != ((modemState[0] & 0x10) == 0x10))
                    {
                        ctsState = !ctsState;
                        if (ctsCallback != null)
                            ctsCallback.onCTSChanged(ctsState);
                    }
                }

                // Check DSR status
                if(dtrDsrEnabled)
                {
                    if(dsrState != ((modemState[0] & 0x20) == 0x20))
                    {
                        dsrState = !dsrState;
                        if (dsrCallback != null)
                            dsrCallback.onDSRChanged(dsrState);
                    }
                }

                //Check Parity Errors
                if(parityCallback != null)
                {
                    if((commStatus[0] & 0x10) == 0x10)
                    {
                        parityCallback.onParityError();
                    }
                }

                // Check frame error
                if(frameCallback != null)
                {
                    if((commStatus[0] & 0x02) == 0x02)
                    {
                        frameCallback.onFramingError();
                    }
                }

                // Check break interrupt
                if(breakCallback != null)
                {
                    if((commStatus[0] & 0x01) == 0x01)
                    {
                        breakCallback.onBreakInterrupt();
                    }
                }

                // Check Overrun error

                if(overrunCallback != null)
                {
                    if((commStatus[0] & 0x04) == 0x04
                        || (commStatus[0] & 0x8) == 0x08)
                    {
                        overrunCallback.onOverrunError();
                    }

                }
            }else // Execute the callback always the first time
            {
                if(rtsCtsEnabled && ctsCallback != null)
                    ctsCallback.onCTSChanged(ctsState);

                if(dtrDsrEnabled && dsrCallback != null)
                    dsrCallback.onDSRChanged(dsrState);

                firstTime = false;
            }
        }

        private byte[] pollLines()
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

            return getModemState();
        }
    }

    private boolean openCP2102()
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
            }else
            {
                outEndpoint = endpoint;
            }
        }


        // Default Setup
        if(setControlCommand(CP210x_IFC_ENABLE, CP210x_UART_ENABLE, null) < 0)
            return false;
        setBaudRate(DEFAULT_BAUDRATE);
        if(setControlCommand(CP210x_SET_LINE_CTL, CP210x_LINE_CTL_DEFAULT,null) < 0)
            return false;
        setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
        if(setControlCommand(CP210x_SET_MHS, CP210x_MHS_DEFAULT, null) < 0)
            return false;

        return true;
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

    private int setControlCommand(int request, int value, byte[] data)
    {
        int dataLength = 0;
        if(data != null)
        {
            dataLength = data.length;
        }
        int response = connection.controlTransfer(CP210x_REQTYPE_HOST2DEVICE, request, value, mInterface.getId(), data, dataLength, USB_TIMEOUT);
        Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
        return response;
    }

    private byte[] getModemState()
    {
        byte[] data = new byte[1];
        connection.controlTransfer(CP210x_REQTYPE_DEVICE2HOST, CP210x_GET_MDMSTS, 0, mInterface.getId(), data, 1, USB_TIMEOUT);
        return data;
    }

    private byte[] getCommStatus()
    {
        byte[] data = new byte[19];
        int response = connection.controlTransfer(CP210x_REQTYPE_DEVICE2HOST, CP210x_GET_COMM_STATUS, 0, mInterface.getId(), data, 19, USB_TIMEOUT);
        Log.i(CLASS_ID, "Control Transfer Response (Comm status): " + String.valueOf(response));
        return data;
    }

    private short getCTL()
    {
        byte[] data = new byte[2];
        int response = connection.controlTransfer(CP210x_REQTYPE_DEVICE2HOST, CP210x_GET_LINE_CTL, 0, mInterface.getId(), data, data.length, USB_TIMEOUT);
        Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
        return (short)((data[1] << 8) | (data[0] & 0xFF));
    }
}