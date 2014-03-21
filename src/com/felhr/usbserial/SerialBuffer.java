package com.felhr.usbserial;

public class SerialBuffer 
{
	private static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;
	private static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
	
	private byte[] writeBuffer;
	private byte[] readBuffer;
	
	private Object mReadLock;
	private Object mWriteLock;
	
	public SerialBuffer(Object mReadLock, Object mWriteLock)
	{
		writeBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
		readBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
		this.mReadLock = mReadLock;
		this.mWriteLock = mWriteLock;
	}
	
	public void putWriteBuffer(byte[] data)
	{
		synchronized(mWriteLock)
		{
			// TO-DO
		}
	}
	
	public byte[] getWriteBuffer()
	{
		// TO-DO
		return null;
	}

}
