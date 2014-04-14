package com.felhr.usbserial;

import java.util.concurrent.atomic.AtomicBoolean;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public abstract class UsbSerialDevice implements UsbSerialInterface
{
	private static final String CLASS_ID = UsbSerialDevice.class.getSimpleName();
	
	protected final UsbDevice device;
	protected final UsbDeviceConnection connection;
	
	protected static final int USB_TIMEOUT = 5000;
	
	protected SerialBuffer serialBuffer;
	
	protected WorkerThread workerThread;
	
	public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
	{
		this.device = device;
		this.connection = connection;
		serialBuffer = new SerialBuffer();
		workerThread = new WorkerThread();
		workerThread.start();
	}
	
	// Common Usb Serial Operations (I/O Asynchronous)
	@Override
	public abstract void open();
	@Override
	public abstract void write(byte[] buffer);
	@Override
	public abstract int read(UsbReadCallback mCallback);
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
	
	/*
	 * WorkerThread waits for request notifications from IN endpoint
	 */
	protected class WorkerThread extends Thread
	{
		private UsbReadCallback callback;
		private UsbRequest requestIN;
		private AtomicBoolean working;
		
		public WorkerThread()
		{
			working = new AtomicBoolean(true);
		}
		
		@Override
		public void run()
		{
			while(working.get())
			{
				UsbRequest request = connection.requestWait();
				if(request != null && request.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
						&& request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN)
				{
					byte[] data = serialBuffer.getDataReceived();
					serialBuffer.clearReadBuffer();
					onReceivedData(data);
					requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
				}
			}
		}
		
		public void setCallback(UsbReadCallback callback)
		{
			this.callback = callback;
		}
		
		public void setUsbRequest(UsbRequest request)
		{
			this.requestIN = request;
		}
		
		private void onReceivedData(byte[] data)
		{
			callback.onReceivedData(data);
		}
		
		public void stopWorkingThread()
		{
			working.set(false);
		}
		
	}

	/*
	 * Kill workingThread; This must be called when closing a device
	 */
	protected void killWorkingThread()
	{
		if(workerThread != null)
		{
			workerThread.stopWorkingThread();
			workerThread = null;
		}
	}
	
	/*
	 * Restart workingThread if it has been killed before
	 */
	protected void restartWorkingThread()
	{
		if(workerThread == null)
		{
			workerThread = new WorkerThread();
			workerThread.start();
		}
	}
}
