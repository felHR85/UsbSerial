package com.felhr.usbserial;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SerialBuffer 
{
	public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
	public static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;
	private ByteBuffer readBuffer;
	private WriteBuffer writeBuffer;
	
	public SerialBuffer()
	{
		readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
		writeBuffer = new WriteBuffer();
	}
	
	public void putReadBuffer(ByteBuffer data)
	{
		synchronized(this)
		{
			try
			{
				readBuffer.put(data);
			}catch(BufferOverflowException e)
			{
				// TO-DO
			}
		}
	}
	
	public ByteBuffer getReadBuffer()
	{
		synchronized(this)
		{
			return readBuffer;
		}
	}
	
	public byte[] getDataReceived()
	{
		synchronized(this)
		{
			byte[] dst = new byte[readBuffer.position()];
			readBuffer.position(0);
			readBuffer.get(dst, 0, dst.length);
			return dst;
		}
	}
	
	public void clearReadBuffer()
	{
		synchronized(this)
		{
			readBuffer.clear();
		}
	}
	
	public byte[] getWriteBuffer()
	{
		return writeBuffer.get();
	}
	
	public void putWriteBuffer(byte[]data)
	{
		writeBuffer.put(data);
	}
	
	public void resetWriteBuffer()
	{
		writeBuffer.reset();
	}
	
	private class WriteBuffer
	{
		private byte[] writeBuffer;
		private int position;
		
		public WriteBuffer()
		{
			this.writeBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
			position = -1;
		}
		
		public synchronized void put(byte[] src)
		{
			if(position == -1)
				position = 0;
			System.arraycopy(src, 0, writeBuffer, position, src.length);
			position += src.length;
			notify();
		}
		
		public synchronized byte[] get()
		{
			if(position == -1)
			{ 
				try 
				{
					wait();
				} catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
			byte[] dst =  Arrays.copyOfRange(writeBuffer, 0, position);
			position = -1;
			return dst;
		}
		
		public synchronized void reset()
		{
			position = -1;
		}
	}

}
