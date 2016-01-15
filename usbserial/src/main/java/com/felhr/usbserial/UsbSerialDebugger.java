package com.felhr.usbserial;

import com.felhr.utils.HexData;

import android.util.Log;

public class UsbSerialDebugger
{
    private static final String CLASS_ID = UsbSerialDebugger.class.getSimpleName();
    public static final String ENCODING = "UTF-8";

    private UsbSerialDebugger()
    {

    }

    public static void printLogGet(byte[] src, boolean verbose)
    {
        if(!verbose)
        {
            Log.i(CLASS_ID, "Data obtained from write buffer: " + new String(src));
        }else
        {
            Log.i(CLASS_ID, "Data obtained from write buffer: " + new String(src));
            Log.i(CLASS_ID, "Raw data from write buffer: " + HexData.hexToString(src));
            Log.i(CLASS_ID, "Number of bytes obtained from write buffer: " + src.length);
        }
    }

    public static void printLogPut(byte[] src, boolean verbose)
    {
        if(!verbose)
        {
            Log.i(CLASS_ID, "Data obtained pushed to write buffer: " + new String(src));
        }else
        {
            Log.i(CLASS_ID, "Data obtained pushed to write buffer: " + new String(src));
            Log.i(CLASS_ID, "Raw data pushed to write buffer: " + HexData.hexToString(src));
            Log.i(CLASS_ID, "Number of bytes pushed from write buffer: " + src.length);
        }
    }

    public static void printReadLogGet(byte[] src, boolean verbose)
    {
        if(!verbose)
        {
            Log.i(CLASS_ID, "Data obtained from Read buffer: " + new String(src));
        }else
        {
            Log.i(CLASS_ID, "Data obtained from Read buffer: " + new String(src));
            Log.i(CLASS_ID, "Raw data from Read buffer: " + HexData.hexToString(src));
            Log.i(CLASS_ID, "Number of bytes obtained from Read buffer: " + src.length);
        }
    }

    public static void printReadLogPut(byte[] src, boolean verbose)
    {
        if(!verbose)
        {
            Log.i(CLASS_ID, "Data obtained pushed to read buffer: " + new String(src));
        }else
        {
            Log.i(CLASS_ID, "Data obtained pushed to read buffer: " + new String(src));
            Log.i(CLASS_ID, "Raw data pushed to read buffer: " + HexData.hexToString(src));
            Log.i(CLASS_ID, "Number of bytes pushed from read buffer: " + src.length);
        }
    }



}
