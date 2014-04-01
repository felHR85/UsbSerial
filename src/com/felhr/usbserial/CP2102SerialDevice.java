package com.felhr.usbserial;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

public class CP2102SerialDevice extends UsbSerialDevice
{
	private static final int CP210x_IFC_ENABLE = 0x00;
	private static final int CP210x_SET_BAUDDIV = 0x01;
	private static final int CP210x_SET_LINE_CTL = 0x03;
	private static final int CP210x_SET_MHS = 0x07;
	private static final int CP210x_SET_BAUDRATE = 0x1E;
	private static final int CP210x_SET_FLOW = 0x13;
	private static final int CP210x_SET_XON = 0x09;
	private static final int CP210x_SET_XOFF = 0x0A;
	
	private static final int CP210x_REQTYPE_HOST2DEVICE = 0x41;
	
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
	
	private UsbInterface mInterface;
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;

	public CP2102SerialDevice(UsbDevice device, UsbDeviceConnection connection) 
	{
		super(device, connection);
		
	}

	@Override
	public void open() 
	{
		// TODO
	}

	@Override
	public void write(byte[] buffer) 
	{
		// TODO 
	}

	// This methods is going to need callback
	@Override
	public int read() 
	{
		// TODO 
		return 0;
	}

	@Override
	public void close() 
	{
		// TODO 
	}

	@Override
	public void setBaudRate(int baudRate) 
	{
		// TODO 
	}

	@Override
	public void setDataBits(int dataBits) 
	{
		// TODO 
	}

	@Override
	public void setStopBits(int stopBits) 
	{
		// TODO 
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
	
	private int setControlCommand(int request, int value, byte[] data)
	{
		return 0;
	}

}
