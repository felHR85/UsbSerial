package com.felhr.usbserial;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

public abstract class UsbSerialDevice implements UsbSerialInterface
{
	private UsbDevice device;
	private UsbDeviceConnection connection;
	
	private Object readBufferLock;
	private Object writeBufferLock;
	
	public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
	{
		this.device = device;
		this.connection = connection;
		this.readBufferLock = new Object();
		this.writeBufferLock = new Object();
	}
	

}
