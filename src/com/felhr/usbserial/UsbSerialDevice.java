package com.felhr.usbserial;

import java.util.Arrays;
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
	protected WriteThread writeThread;
	
	
	public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
	{
		this.device = device;
		this.connection = connection;
		serialBuffer = new SerialBuffer();
		workerThread = new WorkerThread();
		writeThread = new WriteThread();
		workerThread.start();
		writeThread.start();
	}
	
	// Common Usb Serial Operations (I/O Asynchronous)
	@Override
	public abstract void open();
	
	@Override
	public void write(byte[] buffer)
	{
		serialBuffer.putWriteBuffer(buffer);
	}
	
	@Override
	public int read(UsbReadCallback mCallback)
	{
		workerThread.setCallback(mCallback);
		workerThread.getUsbRequest().queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE); 
		return 0;
	}
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
	
	private boolean isFTDIDevice()
	{
		return (this instanceof FTDISerialDevice);
	}
	
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
					// FTDI devices reserves two first bytes of an IN endpoint with info about
					// modem and Line.
					if(isFTDIDevice())
						data = adaptArray(data);
					
					// Clear buffer and execute the callback
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
		
		public UsbRequest getUsbRequest()
		{
			return requestIN;
		}
		
		private void onReceivedData(byte[] data)
		{
			callback.onReceivedData(data);
		}
		
		public void stopWorkingThread()
		{
			working.set(false);
		}
		
		// Special treatment needed to FTDI devices
		private byte[] adaptArray(byte[] ftdiData)
		{
			int length = ftdiData.length;
			if(length > 64)
			{
				int n = 0;
				while(n <= length)
				{
					removeElement(ftdiData, n);
					removeElement(ftdiData, n+1);
					n += 64;
				}
				return ftdiData;
			}else
			{
				return Arrays.copyOfRange(ftdiData, 2, length);
			}
				
		}
		
		private void removeElement(byte[] src, int pos)
		{
		    System.arraycopy(src, pos+1, src, pos, src.length-1-pos);
		}
		
	}
	
	protected class WriteThread extends Thread
	{
		private UsbEndpoint outEndpoint;
		private AtomicBoolean working;
		
		public WriteThread()
		{
			working = new AtomicBoolean(true);
		}
		
		@Override
		public void run()
		{
			while(working.get())
			{
				byte[] data = serialBuffer.getWriteBuffer();
				connection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);
			}
		}
		
		public void setUsbEndpoint(UsbEndpoint outEndpoint)
		{
			this.outEndpoint = outEndpoint;
		}
		
		public void stopWriteThread()
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
	
	protected void killWriteThread()
	{
		if(writeThread != null)
		{
			writeThread.stopWriteThread();
			writeThread = null;
			serialBuffer.resetWriteBuffer();
		}
	}
	
	protected void restartWriteThread()
	{
		if(writeThread == null)
		{
			writeThread = new WriteThread();
			writeThread.start();
		}
	}
}
