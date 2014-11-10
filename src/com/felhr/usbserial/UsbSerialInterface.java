package com.felhr.usbserial;

/**
 * Interface to handle a serial port
 * @author felhr (felhr85@gmail.com)
 *
 */
public interface UsbSerialInterface 
{
	// Common values
	public static final int DATA_BITS_5 = 5;
	public static final int DATA_BITS_6 = 6;
	public static final int DATA_BITS_7 = 7;
	public static final int DATA_BITS_8 = 8;
	
	public static final int STOP_BITS_1 = 1;
	public static final int STOP_BITS_15 = 3;
	public static final int STOP_BITS_2 = 2;
	
	public static final int PARITY_NONE = 0;
	public static final int PARITY_ODD = 1;
	public static final int PARITY_EVEN = 2;
	public static final int PARITY_MARK = 3;
	public static final int PARITY_SPACE = 4;
	
	public static final int FLOW_CONTROL_OFF = 0;
	public static final int FLOW_CONTROL_RTS_CTS= 1;
	public static final int FLOW_CONTROL_DSR_DTR = 2;
	public static final int FLOW_CONTROL_XON_XOFF = 3;
	
	// Common Usb Serial Operations (I/O Asynchronous)
	public boolean open();
	public void write(byte[] buffer);
	public int read(UsbReadCallback mCallback);
	public void close();
	
	// Serial port configuration
	public void setBaudRate(int baudRate);
	public void setDataBits(int dataBits);
	public void setStopBits(int stopBits);
	public void setParity(int parity);
	public void setFlowControl(int flowControl);
	
	// Usb Read Callback
	public interface UsbReadCallback
	{
		public void onReceivedData(byte[] data);
	}
	
}
