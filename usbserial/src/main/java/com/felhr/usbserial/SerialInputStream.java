package com.felhr.usbserial;

import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

public class SerialInputStream extends InputStream implements UsbSerialInterface.UsbReadCallback
{
    protected final UsbSerialInterface device;
    protected ArrayBlockingQueue data = new ArrayBlockingQueue<Integer>(256);
    protected volatile boolean is_open;

    public SerialInputStream(UsbSerialInterface device)
    {
        this.device = device;
        is_open = true;
        device.read(this);
    }

    @Override
    public int read()
    {
        while (is_open)
        {
            try
            {
                return (Integer)data.take();
            } catch (InterruptedException e)
            {
                // ignore, will be retried by while loop
            }
        }
        return -1;
    }

	@Override
	public int read(byte[] b)
	{
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len)
	{
		if(b == null)
			throw new NullPointerException();

		if(off < 0 || len < 0 || off + len > b.length)
			throw new IndexOutOfBoundsException();

		Thread curThread = Thread.currentThread();

		int count;
		for(count = 0; count < len && is_open && !curThread.isInterrupted(); )
		{
			try
			{
				Integer value = (Integer)data.poll();

				if(value != null)
				{
					if(value < 0)
						break; // Stream closed

					b[off + count] = (byte)((int)value);
					++count;
				}
				else
				{
					// No more data is available

					if(count > 0)
						break; // We already got some data, so we can return that

					// Haven't gotten anything yet, so we'll keep trying...
					Thread.sleep(10); // Avoid thrashing the CPU
				}
			}
			catch (InterruptedException e)
			{
				break;
			}
		}

		if(count == 0 && !is_open)
			return -1;

		return count;
	}
    
    public void close()
    {
        is_open = false;
        try
        {
            data.put(-1);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public void onReceivedData(byte[] new_data)
    {
        for (byte b : new_data)
        {
            try
            {
                data.put(((int)b) & 0xff);
            } catch (InterruptedException e)
            {
                // ignore, possibly losing bytes when buffer is full
            }
        }
    }
}
