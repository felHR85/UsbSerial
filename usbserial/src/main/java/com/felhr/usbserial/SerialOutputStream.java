package com.felhr.usbserial;

import java.io.OutputStream;
import java.util.Arrays;

public class SerialOutputStream extends OutputStream
{
    protected final UsbSerialInterface device;

    public SerialOutputStream(UsbSerialInterface device)
    {
        this.device = device;
    }

    @Override
    public void write(int b)
    {
        device.write(new byte[] { (byte)b });
    }

    @Override
    public void write(byte[] b)
    {
        device.write(b);
    }
}
