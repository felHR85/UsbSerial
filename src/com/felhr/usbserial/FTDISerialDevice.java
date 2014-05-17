package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public class FTDISerialDevice extends UsbSerialDevice
{
	private static final String CLASS_ID = FTDISerialDevice.class.getSimpleName();
	
	private static final int FTDI_SIO_RESET = 0;
	private static final int FTDI_SIO_MODEM_CTRL = 1;
	private static final int FTDI_SIO_SET_FLOW_CTRL = 2;
	private static final int FTDI_SIO_SET_BAUD_RATE = 3;
	private static final int FTDI_SIO_SET_DATA = 4;
	
	private static final int FTDI_REQTYPE_HOST2DEVICE = 0x40;
	
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
	
	/***
	 *  Default Serial Configuration
	 *  Baud rate: 9600
	 *  Data bits: 8
	 *  Stop bits: 1
	 *  Parity: None
	 *  Flow Control: Off
	 */
	private static final int FTDI_SET_DATA_DEFAULT = 0x0008;
	private static final int FTDI_SET_MODEM_CTRL_DEFAULT1 = 0x0101;
	private static final int FTDI_SET_MODEM_CTRL_DEFAULT2 = 0x0202;
	private static final int FTDI_SET_MODEM_CTRL_DEFAULT3 = 0x0100;
	private static final int FTDI_SET_MODEM_CTRL_DEFAULT4 = 0x0200;
	private static final int FTDI_SET_FLOW_CTRL_DEFAULT = 0x0000;
	
	private UsbInterface mInterface;
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;
	private UsbRequest requestIN;
	
	
	public FTDISerialDevice(UsbDevice device, UsbDeviceConnection connection) 
	{
		super(device, connection);
	}

	@Override
	public void open() 
	{
		// Restart the working thread and writeThread if it has been killed before and claim interface
		restartWorkingThread();
		restartWriteThread();
		mInterface = device.getInterface(0); // FTDI devices have only one interface

		if(connection.claimInterface(mInterface, true))
		{
			Log.i(CLASS_ID, "Interface succesfully claimed");
		}else
		{
			Log.i(CLASS_ID, "Interface could not be claimed");
		}
		
		// Assign endpoints
		int numberEndpoints = mInterface.getEndpointCount();
		for(int i=0;i<=numberEndpoints-1;i++)
		{
			UsbEndpoint endpoint = mInterface.getEndpoint(i);
			if(endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
					&& endpoint.getDirection() == UsbConstants.USB_DIR_IN)
			{
				inEndpoint = endpoint;
			}else
			{
				outEndpoint = endpoint;
			}
		}
		
		// Default Setup
		setControlCommand(FTDI_SIO_RESET, 0x00, null);
		setControlCommand(FTDI_SIO_SET_DATA, FTDI_SET_DATA_DEFAULT, null);
		setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT1, null);
		setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT2, null);
		setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, null);
		setControlCommand(FTDI_SIO_SET_BAUD_RATE, FTDI_BAUDRATE_9600, null);

		// Initialize UsbRequest
		requestIN = new UsbRequest();
		requestIN.initialize(connection, inEndpoint);

		// Pass references to the threads
		workerThread.setUsbRequest(requestIN);
		writeThread.setUsbEndpoint(outEndpoint);
	}

	@Override
	public void close() 
	{
		setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT3, null);
		setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT4, null);
		killWorkingThread();
		killWriteThread();
		connection.close();
	}

	@Override
	public void setBaudRate(int baudRate) 
	{
		if(baudRate == FTDI_BAUDRATE_300 || baudRate == FTDI_BAUDRATE_600 || baudRate == FTDI_BAUDRATE_1200 ||
				baudRate == FTDI_BAUDRATE_2400 || baudRate == FTDI_BAUDRATE_4800 || baudRate == FTDI_BAUDRATE_9600 ||
				baudRate == FTDI_BAUDRATE_19200 || baudRate == FTDI_BAUDRATE_38400 || baudRate == FTDI_BAUDRATE_57600 ||
				baudRate == FTDI_BAUDRATE_115200 || baudRate == FTDI_BAUDRATE_230400 || baudRate == FTDI_BAUDRATE_460800 ||
				baudRate == FTDI_BAUDRATE_921600)
		{
			setControlCommand(FTDI_SIO_SET_BAUD_RATE, baudRate, null);
		}else
		{
			setControlCommand(FTDI_SIO_SET_BAUD_RATE, FTDI_BAUDRATE_9600, null);
		}
		
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
	
	private int setControlCommand(int request, int value, byte[] data)
	{
		int dataLength = 0;
		if(data != null)
		{
			dataLength = data.length;
		}
		int response = connection.controlTransfer(FTDI_REQTYPE_HOST2DEVICE, request, value, 0, data, dataLength, USB_TIMEOUT);
		Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
		return response;
	}

}
