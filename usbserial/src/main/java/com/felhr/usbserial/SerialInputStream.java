package com.felhr.usbserial;

import java.io.IOException;
import java.io.InputStream;

public class SerialInputStream extends InputStream
{
    private int timeout = 0;

    private int bufferSize =  16 * 1024;

    private byte[] buffer;

    protected final UsbSerialInterface device;

    public SerialInputStream(UsbSerialInterface device)
    {
        this.device = device;
        this.buffer = new byte[bufferSize];
    }

    public SerialInputStream(UsbSerialInterface device, int bufferSize)
    {
        this.device = device;
        this.bufferSize = bufferSize;
        this.buffer = new byte[this.bufferSize];
    }

    @Override
    public int read()
    {
        byte[] buffer = new byte[bufferSize];
        int ret = device.syncRead(buffer, timeout);
        if(ret >= 0)
            return buffer[0];
        else
            return -1;
    }

    @Override
    public int read(byte[] b)
    {
        return device.syncRead(b, timeout);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
