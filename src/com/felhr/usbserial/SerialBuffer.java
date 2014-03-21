package com.felhr.usbserial;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class SerialBuffer 
{
	private static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;
	private static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
	
	private ByteBuffer writeBuffer;
	private ByteBuffer readBuffer;	
	private Object mReadLock;
	private Object mWriteLock;
	
	public SerialBuffer(Object mReadLock, Object mWriteLock)
	{
		writeBuffer = ByteBuffer.allocate(DEFAULT_WRITE_BUFFER_SIZE);
		readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
		this.mReadLock = mReadLock;
		this.mWriteLock = mWriteLock;
	}
	
	public void putWriteBuffer(byte[] data)
	{
		synchronized(mWriteLock)
		{
			try
			{
				writeBuffer.put(ByteBuffer.wrap(data));
			}catch(BufferOverflowException e)
			{
				if(data.length > DEFAULT_WRITE_BUFFER_SIZE && writeBuffer.position() == 0)
				{
					writeBuffer.put(data, 0, DEFAULT_WRITE_BUFFER_SIZE);
				}else if(data.length > DEFAULT_WRITE_BUFFER_SIZE && writeBuffer.position() > 0)
				{
					writeBuffer.put(data, 0, DEFAULT_WRITE_BUFFER_SIZE -
							writeBuffer.position());
				}
			}
		}
	}
	
	public ByteBuffer getWriteBuffer()
	{
		synchronized(mWriteLock)
		{
			ByteBuffer buff = writeBuffer;
			writeBuffer.clear();
			return buff;
		}
	}
	
	public void putReadBuffer(ByteBuffer data)
	{
		// TO-DO
	}
	
	public byte[] getReadBuffer()
	{
		// TO-DO
		return null;
	}

}
