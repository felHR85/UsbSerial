package com.felhr.utils;

import android.hardware.usb.UsbRequest;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;


public class SafeUsbRequest extends UsbRequest
{
    static final String usbRqBufferField = "mBuffer";
    static final String usbRqLengthField = "mLength";

    @Override
    public boolean queue(ByteBuffer buffer, int length)
    {
        Field usbRequestBuffer;
        Field usbRequestLength;
        try
        {
            usbRequestBuffer = UsbRequest.class.getDeclaredField(usbRqBufferField);
            usbRequestLength = UsbRequest.class.getDeclaredField(usbRqLengthField);
            usbRequestBuffer.setAccessible(true);
            usbRequestLength.setAccessible(true);
            usbRequestBuffer.set(this, buffer);
            usbRequestLength.set(this, length);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return super.queue(buffer, length);
    }
}
