package com.felhr.usbserial;

/**
 * Interface to handle a serial port
 * @author felhr (felhr85@gmail.com)
 *
 */
public interface UsbSerialInterface
{
    // Common values
    int DATA_BITS_5 = 5;
    int DATA_BITS_6 = 6;
    int DATA_BITS_7 = 7;
    int DATA_BITS_8 = 8;

    int STOP_BITS_1 = 1;
    int STOP_BITS_15 = 3;
    int STOP_BITS_2 = 2;

    int PARITY_NONE = 0;
    int PARITY_ODD = 1;
    int PARITY_EVEN = 2;
    int PARITY_MARK = 3;
    int PARITY_SPACE = 4;

    int FLOW_CONTROL_OFF = 0;
    int FLOW_CONTROL_RTS_CTS= 1;
    int FLOW_CONTROL_DSR_DTR = 2;
    int FLOW_CONTROL_XON_XOFF = 3;

    // Common Usb Serial Operations (I/O Asynchronous)
    boolean open();
    void write(byte[] buffer);
    int read(UsbReadCallback mCallback);
    void close();

    // Common Usb Serial Operations (I/O Synchronous)
    boolean syncOpen();
    int syncWrite(byte[] buffer, int timeout);
    int syncRead(byte[] buffer, int timeout);
    void syncClose();

    // Serial port configuration
    void setBaudRate(int baudRate);
    void setDataBits(int dataBits);
    void setStopBits(int stopBits);
    void setParity(int parity);
    void setFlowControl(int flowControl);
    void setBreak(boolean state);

    // Flow control commands and interface callback
    void setRTS(boolean state);
    void setDTR(boolean state);
    void getCTS(UsbCTSCallback ctsCallback);
    void getDSR(UsbDSRCallback dsrCallback);

    // Status methods
    void getBreak(UsbBreakCallback breakCallback);
    void getFrame(UsbFrameCallback frameCallback);
    void getOverrun(UsbOverrunCallback overrunCallback);
    void getParity(UsbParityCallback parityCallback);

    interface UsbCTSCallback
    {
        void onCTSChanged(boolean state);
    }

    interface UsbDSRCallback
    {
        void onDSRChanged(boolean state);
    }

    // Error signals callbacks
    interface UsbBreakCallback
    {
        void onBreakInterrupt();
    }

    interface UsbFrameCallback
    {
        void onFramingError();
    }

    interface  UsbOverrunCallback
    {
        void onOverrunError();
    }

    interface UsbParityCallback
    {
        void onParityError();
    }

    // Usb Read Callback
    interface UsbReadCallback
    {
        void onReceivedData(byte[] data);
    }

}
