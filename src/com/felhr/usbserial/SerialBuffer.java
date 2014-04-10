package com.felhr.usbserial;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class SerialBuffer 
{
	public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
	private ByteBuffer readBuffer;	
	
	public SerialBuffer()
	{
		readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
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

}
