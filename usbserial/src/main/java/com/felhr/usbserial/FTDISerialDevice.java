package com.felhr.usbserial;

import java.util.Arrays;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.Log;

import com.felhr.utils.SafeUsbRequest;

public class FTDISerialDevice extends UsbSerialDevice
{
    private static final String CLASS_ID = FTDISerialDevice.class.getSimpleName();

    private static final int FTDI_SIO_RESET = 0;
    private static final int FTDI_SIO_MODEM_CTRL = 1;
    private static final int FTDI_SIO_SET_FLOW_CTRL = 2;
    private static final int FTDI_SIO_SET_BAUD_RATE = 3;
    private static final int FTDI_SIO_SET_DATA = 4;

    private static final int FTDI_REQTYPE_HOST2DEVICE = 0x40;

    /**
     *  RTS and DTR values obtained from FreeBSD FTDI driver
     *  https://github.com/freebsd/freebsd/blob/70b396ca9c54a94c3fad73c3ceb0a76dffbde635/sys/dev/usb/serial/uftdi_reg.h
     */
    private static final int FTDI_SIO_SET_DTR_MASK = 0x1;
    private static final int FTDI_SIO_SET_DTR_HIGH = (1 | (FTDI_SIO_SET_DTR_MASK << 8));
    private static final int FTDI_SIO_SET_DTR_LOW = (0 | (FTDI_SIO_SET_DTR_MASK << 8));
    private static final int FTDI_SIO_SET_RTS_MASK = 0x2;
    private static final int FTDI_SIO_SET_RTS_HIGH = (2 | (FTDI_SIO_SET_RTS_MASK << 8));
    private static final int FTDI_SIO_SET_RTS_LOW = (0 | (FTDI_SIO_SET_RTS_MASK << 8));

    /**
     *  BREAK on/off values obtained from linux driver
     *  https://github.com/torvalds/linux/blob/master/drivers/usb/serial/ftdi_sio.h
     */
    private static final int FTDI_SIO_SET_BREAK_ON = (1<<14);
    private static final int FTDI_SIO_SET_BREAK_OFF = (0<<14);

    public static final int FTDI_BAUDRATE_300 = 0x2710;
    public static final int FTDI_BAUDRATE_600 = 0x1388;
    public static final int FTDI_BAUDRATE_1200 = 0x09c4;
    public static final int FTDI_BAUDRATE_2400 = 0x04e2;
    public static final int FTDI_BAUDRATE_4800 = 0x0271;
    public static final int FTDI_BAUDRATE_9600 = 0x4138;
    public static final int FTDI_BAUDRATE_19200 = 0x809c;
    public static final int FTDI_BAUDRATE_38400 = 0xc04e;
    public static final int FTDI_BAUDRATE_57600 = 0x0034;
    public static final int FTDI_BAUDRATE_115200 = 0x001a;
    public static final int FTDI_BAUDRATE_230400 = 0x000d;
    public static final int FTDI_BAUDRATE_460800 = 0x4006;
    public static final int FTDI_BAUDRATE_921600 = 0x8003;

    /***
     *  Default Serial Configuration
     *  Baud rate: 9600
     *  Data bits: 8
     *  Stop bits: 1
     *  Parity: None
     *  Flow Control: Off
     */
    private static final int FTDI_SET_DATA_DEFAULT = 0x0008;
    private static final int FTDI_SET_MODEM_CTRL_DEFAULT1 = 0x0101;
    private static final int FTDI_SET_MODEM_CTRL_DEFAULT2 = 0x0202;
    private static final int FTDI_SET_MODEM_CTRL_DEFAULT3 = 0x0100;
    private static final int FTDI_SET_MODEM_CTRL_DEFAULT4 = 0x0200;
    private static final int FTDI_SET_FLOW_CTRL_DEFAULT = 0x0000;
    private static final byte[] EMPTY_BYTE_ARRAY = {};

    private int currentSioSetData = 0x0000;

    /**
     * Flow control variables
     */
    private boolean rtsCtsEnabled;
    private boolean dtrDsrEnabled;

    private boolean ctsState;
    private boolean dsrState;
    private boolean firstTime; // with this flag we set the CTS and DSR state to the first value received from the FTDI device

    private UsbCTSCallback ctsCallback;
    private UsbDSRCallback dsrCallback;

    private final UsbInterface mInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;

    public FTDIUtilities ftdiUtilities;

    private UsbSerialInterface.UsbParityCallback parityCallback;
    private UsbSerialInterface.UsbFrameCallback frameCallback;
    private UsbSerialInterface.UsbOverrunCallback overrunCallback;
    private UsbSerialInterface.UsbBreakCallback breakCallback;

    public FTDISerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        this(device, connection, -1);
    }

    public FTDISerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface)
    {
        super(device, connection);
        ftdiUtilities = new FTDIUtilities();
        rtsCtsEnabled = false;
        dtrDsrEnabled = false;
        ctsState = true;
        dsrState = true;
        firstTime = true;
        mInterface = device.getInterface(iface >= 0 ? iface : 0);
    }

    @Override
    public boolean open()
    {
        boolean ret = openFTDI();

        if(ret)
        {
            // Initialize UsbRequest
            UsbRequest requestIN = new SafeUsbRequest();
            requestIN.initialize(connection, inEndpoint);

            // Restart the working thread if it has been killed before and  get and claim interface
            restartWorkingThread();
            restartWriteThread();

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
        setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT3, 0);
        setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT4, 0);
        currentSioSetData = 0x0000;
        killWorkingThread();
        killWriteThread();
        connection.releaseInterface(mInterface);
        isOpen = false;
    }

    @Override
    public boolean syncOpen()
    {
        boolean ret = openFTDI();
        if(ret)
        {
            setSyncParams(inEndpoint, outEndpoint);
            asyncMode = false;

            // Init Streams
            inputStream = new SerialInputStream(this);
            outputStream = new SerialOutputStream(this);

            isOpen = true;

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
        setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT3, 0);
        setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT4, 0);
        currentSioSetData = 0x0000;
        connection.releaseInterface(mInterface);
        isOpen = false;
    }

    @Override
    public void setBaudRate(int baudRate)
    {
        short[] encodedBaudRate = encodedBaudRate(baudRate);

        if(encodedBaudRate != null) {
            setEncodedBaudRate(encodedBaudRate);
        }else{
            setOldBaudRate(baudRate);
        }
    }

    @Override
    public void setDataBits(int dataBits)
    {
        switch(dataBits)
        {
            case UsbSerialInterface.DATA_BITS_5:
                currentSioSetData |= 1;
                currentSioSetData &= ~(1 << 1);
                currentSioSetData |= (1 << 2);
                currentSioSetData &= ~(1 << 3);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            case UsbSerialInterface.DATA_BITS_6:
                currentSioSetData &= ~1;
                currentSioSetData |= (1 << 1);
                currentSioSetData |= (1 << 2);
                currentSioSetData &= ~(1 << 3);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            case UsbSerialInterface.DATA_BITS_7:
                currentSioSetData |= 1;
                currentSioSetData |= (1 << 1);
                currentSioSetData |= (1 << 2);
                currentSioSetData &= ~(1 << 3);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            case UsbSerialInterface.DATA_BITS_8:
                currentSioSetData &= ~1;
                currentSioSetData &= ~(1 << 1);
                currentSioSetData &= ~(1 << 2);
                currentSioSetData |= (1 << 3);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            default:
                currentSioSetData &= ~1;
                currentSioSetData &= ~(1 << 1);
                currentSioSetData &= ~(1 << 2);
                currentSioSetData |= (1 << 3);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
        }

    }

    @Override
    public void setStopBits(int stopBits)
    {
        switch(stopBits)
        {
            case UsbSerialInterface.STOP_BITS_1:
                currentSioSetData &= ~(1 << 11);
                currentSioSetData &= ~(1 << 12);
                currentSioSetData &= ~(1 << 13);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            case UsbSerialInterface.STOP_BITS_15:
                currentSioSetData |= (1 << 11);
                currentSioSetData &= ~(1 << 12);
                currentSioSetData &= ~(1 << 13);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            case UsbSerialInterface.STOP_BITS_2:
                currentSioSetData &= ~(1 << 11);
                currentSioSetData |= (1 << 12);
                currentSioSetData &= ~(1 << 13);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            default:
                currentSioSetData &= ~(1 << 11);
                currentSioSetData &= ~(1 << 12);
                currentSioSetData &= ~(1 << 13);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
        }

    }

    @Override
    public void setParity(int parity)
    {
        switch(parity)
        {
            case UsbSerialInterface.PARITY_NONE:
                currentSioSetData &= ~(1 << 8);
                currentSioSetData &= ~(1 << 9);
                currentSioSetData &= ~(1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            case UsbSerialInterface.PARITY_ODD:
                currentSioSetData |= (1 << 8);
                currentSioSetData &= ~(1 << 9);
                currentSioSetData &= ~(1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            case UsbSerialInterface.PARITY_EVEN:
                currentSioSetData &= ~(1 << 8);
                currentSioSetData |= (1 << 9);
                currentSioSetData &= ~(1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            case UsbSerialInterface.PARITY_MARK:
                currentSioSetData |= (1 << 8);
                currentSioSetData |= (1 << 9);
                currentSioSetData &= ~(1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            case UsbSerialInterface.PARITY_SPACE:
                currentSioSetData &= ~(1 << 8);
                currentSioSetData &= ~(1 << 9);
                currentSioSetData |= (1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
            default:
                currentSioSetData &= ~(1 << 8);
                currentSioSetData &= ~(1 << 9);
                currentSioSetData &= ~(1 << 10);
                setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
                break;
        }

    }

    @Override
    public void setFlowControl(int flowControl)
    {
        switch(flowControl)
        {
            case UsbSerialInterface.FLOW_CONTROL_OFF:
                setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, 0);
                rtsCtsEnabled = false;
                dtrDsrEnabled = false;
                break;
            case UsbSerialInterface.FLOW_CONTROL_RTS_CTS:
                rtsCtsEnabled = true;
                dtrDsrEnabled = false;
                int indexRTSCTS = 0x0001;
                setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, indexRTSCTS);
                break;
            case UsbSerialInterface.FLOW_CONTROL_DSR_DTR:
                dtrDsrEnabled = true;
                rtsCtsEnabled = false;
                int indexDSRDTR = 0x0002;
                setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, indexDSRDTR);
                break;
            case UsbSerialInterface.FLOW_CONTROL_XON_XOFF:
                int indexXONXOFF = 0x0004;
                int wValue = 0x1311;
                setControlCommand(FTDI_SIO_SET_FLOW_CTRL, wValue, indexXONXOFF);
                break;
            default:
                setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, 0);
                break;
        }
    }

    /**
     *  BREAK on/off methods obtained from linux driver
     *  https://github.com/torvalds/linux/blob/master/drivers/usb/serial/ftdi_sio.c
     */
    @Override
    public void setBreak(boolean state)
    {
        if(state){
            currentSioSetData |= FTDI_SIO_SET_BREAK_ON;
        }else{
            currentSioSetData &= ~(FTDI_SIO_SET_BREAK_ON);
        }
        setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0);
    }

    @Override
    public void setRTS(boolean state)
    {
        if(state)
        {
            setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SIO_SET_RTS_HIGH, 0);
        }else
        {
            setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SIO_SET_RTS_LOW, 0);
        }
    }

    @Override
    public void setDTR(boolean state)
    {
        if(state)
        {
            setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SIO_SET_DTR_HIGH, 0);
        }else
        {
            setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SIO_SET_DTR_LOW, 0);
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
    }

    private boolean openFTDI()
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
        firstTime = true;
        if(setControlCommand(FTDI_SIO_RESET, 0x00, 0) < 0)
            return false;
        if(setControlCommand(FTDI_SIO_SET_DATA, FTDI_SET_DATA_DEFAULT, 0) < 0)
            return false;
        currentSioSetData = FTDI_SET_DATA_DEFAULT;
        if(setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT1, 0) < 0)
            return false;
        if(setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT2, 0) < 0)
            return false;
        if(setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, 0) < 0)
            return false;
        if(setControlCommand(FTDI_SIO_SET_BAUD_RATE, FTDI_BAUDRATE_9600, 0) < 0)
            return false;

        // Flow control disabled by default
        rtsCtsEnabled = false;
        dtrDsrEnabled = false;

        return true;
    }

    private int setControlCommand(int request, int value, int index)
    {
        int dataLength = 0;
        int response = connection.controlTransfer(FTDI_REQTYPE_HOST2DEVICE, request, value, mInterface.getId() + 1 + index, null, dataLength, USB_TIMEOUT);
        Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
        return response;
    }

    // Special treatment needed to FTDI devices
    static byte[] adaptArray(byte[] ftdiData)
    {
        int length = ftdiData.length;
        if(length > 64)
        {
            int n = 1;
            int p = 64;
            // Precalculate length without FTDI headers
            while(p < length)
            {
                n++;
                p = n*64;
            }
            int realLength = length - n*2;
            byte[] data = new byte[realLength];
            copyData(ftdiData, data);
            return data;
        }
        else if (length == 2) // special case optimization that returns the same instance.
        {
            return EMPTY_BYTE_ARRAY;
        }
        else
        {
            return Arrays.copyOfRange(ftdiData, 2, length);
        }
    }

    // Copy data without FTDI headers
    private static void copyData(byte[] src, byte[] dst)
    {
        int srcPos = 2, dstPos = 0;
        while(srcPos - 2 <= src.length - 64)
        {
            System.arraycopy(src, srcPos, dst, dstPos, 62);
            srcPos += 64;
            dstPos += 62;
        }
        int remaining = src.length - srcPos + 2;
        if (remaining > 0)
        {
            System.arraycopy(src, srcPos, dst, dstPos, remaining - 2);
        }
    }

    public class FTDIUtilities
    {
        // Special treatment needed to FTDI devices
        public byte[] adaptArray(byte[] ftdiData)
        {
            int length = ftdiData.length;
            if(length > 64)
            {
                int n = 1;
                int p = 64;
                // Precalculate length without FTDI headers
                while(p < length)
                {
                    n++;
                    p = n*64;
                }
                int realLength = length - n*2;
                byte[] data = new byte[realLength];
                copyData(ftdiData, data);
                return data;
            }else
            {
                return Arrays.copyOfRange(ftdiData, 2, length);
            }
        }

        public void checkModemStatus(byte[] data)
        {
            if(data.length == 0) // Safeguard for zero length arrays
                return;

            boolean cts = (data[0] & 0x10) == 0x10;
            boolean dsr = (data[0] & 0x20) == 0x20;

            if(firstTime) // First modem status received
            {
                ctsState = cts;
                dsrState = dsr;

                if(rtsCtsEnabled && ctsCallback != null)
                    ctsCallback.onCTSChanged(ctsState);

                if(dtrDsrEnabled && dsrCallback != null)
                    dsrCallback.onDSRChanged(dsrState);

                firstTime = false;
                return;
            }

            if(rtsCtsEnabled &&
                    cts != ctsState && ctsCallback != null) //CTS
            {
                ctsState = !ctsState;
                ctsCallback.onCTSChanged(ctsState);
            }

            if(dtrDsrEnabled &&
                    dsr != dsrState && dsrCallback != null) //DSR
            {
                dsrState = !dsrState;
                dsrCallback.onDSRChanged(dsrState);
            }

            if(parityCallback != null) // Parity error checking
            {
                if((data[1] & 0x04) == 0x04)
                {
                    parityCallback.onParityError();
                }
            }

            if(frameCallback != null) // Frame error checking
            {
                if((data[1] & 0x08) == 0x08)
                {
                    frameCallback.onFramingError();
                }
            }

            if(overrunCallback != null) // Overrun error checking
            {
                if((data[1] & 0x02) == 0x02)
                {
                    overrunCallback.onOverrunError();
                }
            }

            if(breakCallback != null) // Break interrupt checking
            {
                if((data[1] & 0x10) == 0x10)
                {
                    breakCallback.onBreakInterrupt();
                }
            }
        }
    }

    @Override
    public int syncRead(byte[] buffer, int timeout)
    {
        long beginTime = System.currentTimeMillis();
        long stopTime = beginTime + timeout;

        if(asyncMode)
        {
            return -1;
        }

        if(buffer == null)
        {
            return 0;
        }

        int n = buffer.length / 62;
        if(buffer.length % 62 != 0)
        {
            n++;
        }

        byte[] tempBuffer = new byte[buffer.length + n * 2];

        int readen = 0;

        do
        {
            int timeLeft = 0;
            if(timeout > 0)
            {
                timeLeft = (int) (stopTime - System.currentTimeMillis());
                if (timeLeft <= 0)
                {
                    break;
                }
            }

            int numberBytes = connection.bulkTransfer(inEndpoint, tempBuffer, tempBuffer.length, timeLeft);

            if(numberBytes > 2) // Data received
            {
                byte[] newBuffer = this.ftdiUtilities.adaptArray(tempBuffer);
                System.arraycopy(newBuffer, 0, buffer, 0, buffer.length);

                int p = numberBytes / 64;
                if(numberBytes % 64 != 0)
                {
                    p++;
                }
                readen = numberBytes - p * 2;
            }
        }while(readen <= 0);

        return readen;
    }

    @Override
    public int syncRead(byte[] buffer, int offset, int length, int timeout) {

        long beginTime = System.currentTimeMillis();
        long stopTime = beginTime + timeout;

        if(asyncMode)
        {
            return -1;
        }

        if(buffer == null)
        {
            return 0;
        }

        int n = length / 62;
        if(length % 62 != 0)
        {
            n++;
        }

        byte[] tempBuffer = new byte[length + n * 2];

        int readen = 0;

        do
        {
            int timeLeft = 0;
            if(timeout > 0)
            {
                timeLeft = (int) (stopTime - System.currentTimeMillis());
                if (timeLeft <= 0)
                {
                    break;
                }
            }

            int numberBytes = connection.bulkTransfer(inEndpoint, tempBuffer, tempBuffer.length, timeLeft);

            if(numberBytes > 2) // Data received
            {
                byte[] newBuffer = this.ftdiUtilities.adaptArray(tempBuffer);
                System.arraycopy(newBuffer, 0, buffer, offset, length);

                int p = numberBytes / 64;
                if(numberBytes % 64 != 0)
                {
                    p++;
                }
                readen = numberBytes - p * 2;
            }
        }while(readen <= 0);

        return readen;
    }

    private static final byte[] skip = new byte[2];

    /**
     * This method avoids creation of garbage by reusing the same
     * array instance for skipping header bytes and running
     * {@link UsbDeviceConnection#bulkTransfer(UsbEndpoint, byte[], int, int, int)}
     * directly.
     */
    @SuppressLint("NewApi")
    private int readSyncJelly(byte[] buffer, int timeout, long stopTime) {
        int read = 0;
        do
        {
            int timeLeft = 0;
            if(timeout > 0)
            {
                timeLeft = (int) (stopTime - System.currentTimeMillis());
                if (timeLeft <= 0)
                {
                    break;
                }
            }


            int numberBytes = connection.bulkTransfer(inEndpoint, skip, skip.length, timeLeft);

            if(numberBytes > 2) // Data received
            {
                numberBytes = connection.bulkTransfer(inEndpoint, buffer, read, 62, timeLeft);
                read += numberBytes;
            }
        } while(read <= 0);

        return read;
    }

    // https://stackoverflow.com/questions/47303802/how-is-androids-string-usbdevice-getversion-encoded-from-word-bcddevice
    private short getBcdDevice() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            byte[] descriptors = connection.getRawDescriptors();
            return (short) ((descriptors[13] << 8) + descriptors[12]);
        }else{
            return -1;
        }
    }

    private byte getISerialNumber(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            byte[] descriptors = connection.getRawDescriptors();
            return descriptors[16];
        }else{
            return -1;
        }
    }

    private boolean isBaudTolerated(long speed, long target) {
        return ((speed >= (target * 100) / 103) &&
                (speed <= (target * 100) / 97));
    }

    // Encoding baudrate as freebsd driver:
    // https://github.com/freebsd/freebsd/blob/1d6e4247415d264485ee94b59fdbc12e0c566fd0/sys/dev/usb/serial/uftdi.c
    private short[] encodedBaudRate(int baudRate){
        boolean isFT232A = false;
        boolean clk12MHz = false;
        boolean hIndex = false;

        short[] ret = new short[2];
        int clk, divisor, fastClk, frac, hwSpeed;

        byte[] encodedFraction = new byte[]{
                0, 3, 2, 4, 1, 5, 6, 7
        };

        byte[] roundoff232a = new byte[]{
                0,  1,  0,  1,  0, -1,  2,  1,
                0, -1, -2, -3,  4,  3,  2,  1,
        };

        short bcdDevice = getBcdDevice();

        if(bcdDevice == -1) {
            return null;
        }

        if(bcdDevice == 0x200 && getISerialNumber() == 0) {
            isFT232A = true;
        }

        if(bcdDevice == 0x500 || bcdDevice == 0x700 || bcdDevice == 0x800 || bcdDevice == 0x900 || bcdDevice == 0x1000) {
            hIndex = true;
        }

        if(bcdDevice == 0x700 || bcdDevice == 0x800 || bcdDevice == 0x900 ) {
            clk12MHz = true;
        }

        if(baudRate >= 1200 && clk12MHz) {
            clk = 12000000;
            fastClk = (1 << 17);
        }else {
            clk = 3000000;
            fastClk = 0;
        }

        if(baudRate < (clk >> 14) || baudRate > clk) {
            return null;
        }

        divisor = (clk << 4) / baudRate;
        if((divisor & 0xf) == 1) {
            divisor &= 0xfffffff8;
        }else if (isFT232A) {
            divisor += roundoff232a[divisor & 0x0f];
        }else {
            divisor += 1;  /* Rounds odd 16ths up to next 8th. */
        }
        divisor >>= 1;

        hwSpeed = (clk << 3) / divisor;

        if(!isBaudTolerated(hwSpeed, baudRate)) {
            return null;
        }

        frac = divisor & 0x07;
        divisor >>= 3;
        if (divisor == 1) {
            if (frac == 0) {
                divisor = 0;  /* 1.0 becomes 0.0 */
            }else {
                frac = 0;     /* 1.5 becomes 1.0 */
            }
        }
        divisor |= (encodedFraction[frac] << 14) | fastClk;

        ret[0] = (short) divisor; //loBits
        ret[1] = hIndex ?
                (short) ((divisor >> 8) & 0xFF00 | (mInterface.getId() + 1))
                : (short) (divisor >> 16); //hiBits

        return ret;
    }

    private void setEncodedBaudRate(short[] encodedBaudRate) {
        connection.controlTransfer(FTDI_REQTYPE_HOST2DEVICE, FTDI_SIO_SET_BAUD_RATE
                , encodedBaudRate[0], encodedBaudRate[1], null, 0, USB_TIMEOUT);
    }

    private void setOldBaudRate(int baudRate) {
        int value = 0;
        if(baudRate >= 0 && baudRate <= 300 )
            value = FTDI_BAUDRATE_300;
        else if(baudRate > 300 && baudRate <= 600)
            value = FTDI_BAUDRATE_600;
        else if(baudRate > 600 && baudRate <= 1200)
            value = FTDI_BAUDRATE_1200;
        else if(baudRate > 1200 && baudRate <= 2400)
            value = FTDI_BAUDRATE_2400;
        else if(baudRate > 2400 && baudRate <= 4800)
            value = FTDI_BAUDRATE_4800;
        else if(baudRate > 4800 && baudRate <= 9600)
            value = FTDI_BAUDRATE_9600;
        else if(baudRate > 9600 && baudRate <=19200)
            value = FTDI_BAUDRATE_19200;
        else if(baudRate > 19200 && baudRate <= 38400)
            value = FTDI_BAUDRATE_38400;
        else if(baudRate > 19200 && baudRate <= 57600)
            value = FTDI_BAUDRATE_57600;
        else if(baudRate > 57600 && baudRate <= 115200)
            value = FTDI_BAUDRATE_115200;
        else if(baudRate > 115200 && baudRate <= 230400)
            value = FTDI_BAUDRATE_230400;
        else if(baudRate > 230400 && baudRate <= 460800)
            value = FTDI_BAUDRATE_460800;
        else if(baudRate > 460800 && baudRate <= 921600)
            value = FTDI_BAUDRATE_921600;
        else if(baudRate > 921600)
            value = FTDI_BAUDRATE_921600;
        else
            value = FTDI_BAUDRATE_9600;

        setControlCommand(FTDI_SIO_SET_BAUD_RATE, value, 0);
    }
}
