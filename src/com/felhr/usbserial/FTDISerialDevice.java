package com.felhr.usbserial;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

public class FTDISerialDevice extends UsbSerialDevice
{
	private static final String CLASS_ID = FTDISerialDevice.class.getSimpleName();
	
	private static final int FTDI_SIO_RESET = 0;
	private static final int FTDI_SIO_MODEM_CTRL = 1;
	private static final int FTDI_SIO_SET_FLOW_CTRL = 2;
	private static final int FTDI_SIO_SET_BAUD_RATE = 3;
	private static final int FTDI_SIO_SET_DATA = 4;
	
	public static final int FTDI_BAUDRATE_300 = 0x2710;
	public static final int FTDI_BAUDRATE_600 = 0x1388;
	public static final int FTDI_BAUDRATE_1200 = 0x09c4;
	public static final int FTDI_BAUDRATE_2400 = 0x04e2;
	public static final int FTDI_BAUDRATE_4800 = 0x0271;
	public static final int FTDI_BAUDRATE_9600 = 0x4138;
	public static final int FTDI_BAUDRATE_19200 = 0x809c;
	public static final int FTDI_BAUDRATE_38400 = 0xc04e;
	public static final int FTDI_BAUDRATE_57600 = 0x0034;
	public static final int FTDI_BAUDRATE_115200 = 0x001a;
	public static final int FTDI_BAUDRATE_230400 = 0x000d;
	public static final int FTDI_BAUDRATE_460800 = 0x4006;
	public static final int FTDI_BAUDRATE_921600 = 0x8003;
	
	
	public FTDISerialDevice(UsbDevice device, UsbDeviceConnection connection) 
	{
		super(device, connection);
	}

	@Override
	public void open() 
	{
		
		
	}

	@Override
	public void close() 
	{
		
		
	}

	@Override
	public void setBaudRate(int baudRate) 
	{
		
		
	}

	@Override
	public void setDataBits(int dataBits)
	{
		
		
	}

	@Override
	public void setStopBits(int stopBits) 
	{
		
		
	}

	@Override
	public void setParity(int parity) 
	{
		
		
	}

	@Override
	public void setFlowControl(int flowControl) 
	{
		
		
	}

}
