package com.felhr.usbserial;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

public class CP2102SerialDevice extends UsbSerialDevice
{

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

}
