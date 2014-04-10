package com.felhr.usbserial;

import java.util.concurrent.atomic.AtomicBoolean;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public abstract class UsbSerialDevice implements UsbSerialInterface
{
	private static final String CLASS_ID = UsbSerialDevice.class.getSimpleName();
	
	protected final UsbDevice device;
	protected final UsbDeviceConnection connection;
	
	protected SerialBuffer serialBuffer;
	
	private Object objectMonitor;
	protected ListenThread listenThread;
	protected WorkerThread workerThread;
	
	public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
	{
		this.device = device;
		this.connection = connection;
		this.objectMonitor = new Object();
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
	 * WorkerThread waits for request notifications regardless of whether they come from IN or OUT endpoints
	 */
	protected class WorkerThread extends Thread
	{
		private UsbReadCallback callback;
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
				if(request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN) // Read
				{
					byte[] data = serialBuffer.getDataReceived();
					Log.i(CLASS_ID, "Received data length: " + String.valueOf(data.length));
					serialBuffer.clearReadBuffer();
					onReceivedData(data);
					
					synchronized(objectMonitor)
					{
						objectMonitor.notify();
					}
					
				}else // Write
				{
					int pos = serialBuffer.getWriteBuffer().position();
					Log.i(CLASS_ID, "Send data length: "  + String.valueOf(pos + 1));
					serialBuffer.clearWriteBuffer();
				}
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
		
		public void stopWorkingThread()
		{
			working.set(false);
		}
	}
	
	/*
	 * 
	 */
	protected class ListenThread extends Thread
	{
		private UsbRequest requestIN;
		private AtomicBoolean listening;
		
		protected ListenThread(UsbRequest requestIN)
		{
			listening = new AtomicBoolean(true);
			this.requestIN = requestIN;
		}
		
		@Override
		public void run()
		{
			while(listening.get())
			{
				synchronized(objectMonitor)
				{
					try 
					{
						objectMonitor.wait();
					} catch (InterruptedException e) 
					{
						e.printStackTrace();
					}

					requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
				}
			}
			
		}
		
		public void stopListenThread()
		{
			listening.set(false);
		}
		
	}
	
}
