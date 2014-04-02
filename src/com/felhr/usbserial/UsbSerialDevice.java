package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbRequest;

public abstract class UsbSerialDevice implements UsbSerialInterface
{
	protected final UsbDevice device;
	protected final UsbDeviceConnection connection;
	
	protected SerialBuffer serialBuffer;
	protected final Object readBufferLock;
	protected final Object writeBufferLock;
	
	public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
	{
		this.device = device;
		this.connection = connection;
		this.readBufferLock = new Object();
		this.writeBufferLock = new Object();
		serialBuffer = new SerialBuffer(readBufferLock, writeBufferLock);
	}
	
	// Common Usb Serial Operations (I/O Asynchronous)
	@Override
	public abstract void open();
	@Override
	public abstract void write(byte[] buffer);
	@Override
	public abstract int read();
	@Override
	public abstract void close();
	
	// Serial port configuration
	@Override
	public abstract void setBaudRate(int baudRate);
	@Override
	public abstract void setDataBits(int dataBits);
	@Override
	public abstract void setStopBits(int stopBits);
	@Override
	public abstract void setParity(int parity);
	@Override
	public abstract void setFlowControl(int flowControl);
	
	
	protected class WorkerThread extends Thread
	{
		private UsbReadCallback callback;
		
		@Override
		public void run()
		{
			UsbRequest request = connection.requestWait();
			if(request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN) // Read
			{
				
			}else // Write
			{
				
			}
		}
		
		public void setCallback(UsbReadCallback callback)
		{
			this.callback = callback;
		}
		
		private void onReceivedData(byte[] data)
		{
			callback.onReceivedData(data);
		}
	}
	
	public interface UsbReadCallback
	{
		public void onReceivedData(byte[] data);
	}
	
}
