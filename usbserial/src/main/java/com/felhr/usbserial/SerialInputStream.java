package com.felhr.usbserial;

import java.io.IOException;
import java.io.InputStream;

public class SerialInputStream extends InputStream
{
    private int timeout = 0;

    private int maxBufferSize =  16 * 1024;

    private final byte[] buffer;
    private int pointer;
    private int bufferSize;

    protected final UsbSerialInterface device;

    public SerialInputStream(UsbSerialInterface device)
    {
        this.device = device;
        this.buffer = new byte[maxBufferSize];
        this.pointer = 0;
        this.bufferSize = -1;
    }

    public SerialInputStream(UsbSerialInterface device, int maxBufferSize)
    {
        this.device = device;
        this.maxBufferSize = maxBufferSize;
        this.buffer = new byte[this.maxBufferSize];
        this.pointer = 0;
        this.bufferSize = -1;
    }

    @Override
    public int read()
    {
        int value = checkFromBuffer();
        if(value >= 0)
            return value;

        int ret = device.syncRead(buffer, timeout);
        if(ret >= 0) {
            bufferSize = ret;
            return buffer[pointer++] & 0xff;
        }else {
            return -1;
        }
    }

    @Override
    public int read(byte[] b)
    {
        return device.syncRead(b, timeout);
    }

    @Override
    public int available() throws IOException {
        if(bufferSize > 0)
            return bufferSize - pointer;
        else
            return 0;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    private int checkFromBuffer(){
        if(bufferSize > 0 && pointer < bufferSize){
            return buffer[pointer++] & 0xff;
        }else{
            pointer = 0;
            bufferSize = -1;
            return -1;
        }
    }
}
