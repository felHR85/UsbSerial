package com.felhr.usbserial;

import java.io.InputStream;

public class SerialInputStream extends InputStream
{
    private int timeout = 0;

    protected final UsbSerialInterface device;

    public SerialInputStream(UsbSerialInterface device)
    {
        this.device = device;
    }

    @Override
    public int read()
    {
        byte[] buffer = new byte[1];
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
