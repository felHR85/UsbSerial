package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public class PL2303SerialDevice extends UsbSerialDevice
{
	private static final String CLASS_ID = PL2303SerialDevice.class.getSimpleName();
	
	private static final int PL2303_REQTYPE_HOST2DEVICE_VENDOR = 0x40;
	private static final int PL2303_REQTYPE_DEVICE2HOST_VENDOR = 0xC0;
	private static final int PL2303_REQTYPE_HOST2DEVICE = 0x21;
	
	private static final int PL2303_VENDOR_WRITE_REQUEST = 0x01;
	private static final int PL2303_SET_LINE_CODING = 0x20;
	
	private byte[] defaultSetLine = new byte[]{
			(byte) 0x80, // [0:3] Baud rate (reverse hex encoding 9600:00 00 25 80 -> 80 25 00 00)
			(byte) 0x25,
			(byte) 0x00,
			(byte) 0x00,
			(byte) 0x00, // [4] Stop Bits (0=1, 1=1.5, 2=2)
			(byte) 0x00, // [5] Parity (0=NONE 1=ODD 2=EVEN 3=MARK 4=SPACE)
			(byte) 0x08  // [6] Data Bits (5=5, 6=6, 7=7, 8=8)
	};
	
	
	private UsbInterface mInterface;
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;
	private UsbRequest requestIN;
	
	
	public PL2303SerialDevice(UsbDevice device, UsbDeviceConnection connection) 
	{
		super(device, connection);
	}

	@Override
	public void open() 
	{
		// Restart the working thread and writeThread if it has been killed before and claim interface
		restartWorkingThread();
		restartWriteThread();
		mInterface = device.getInterface(0); // PL2303 devices have only one interface

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
		
		//Default Setup
		byte[] buf = new byte[2];
			//Specific vendor stuff that I barely understand but It is on linux drivers, So I trust :)
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0404, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8383, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0404, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x8383, 0, buf);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0000, 0, null);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE_VENDOR, PL2303_VENDOR_WRITE_REQUEST, 0x0001, 0, null);
			// End of specific vendor stuff
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
		
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
		killWorkingThread();
		killWriteThread();
		connection.close();
	}

	@Override
	public void setBaudRate(int baudRate) 
	{
		defaultSetLine[0] = (byte) (baudRate & 0xff);
		defaultSetLine[1] = (byte) (baudRate >> 8 & 0xff);
		defaultSetLine[2] = (byte) (baudRate >> 16 & 0xff);
		defaultSetLine[3] = (byte) (baudRate >> 24 & 0xff);
		
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
	}

	@Override
	public void setDataBits(int dataBits) 
	{
		defaultSetLine[6] = (byte) dataBits;
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
	}

	@Override
	public void setStopBits(int stopBits)
	{
		defaultSetLine[4] = (byte) stopBits;
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);
	}

	@Override
	public void setParity(int parity) 
	{
		defaultSetLine[5] = (byte) parity;
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_SET_LINE_CODING, 0x0000, 0, defaultSetLine);	
	}

	@Override
	public void setFlowControl(int flowControl)
	{
		// TODO
		
	}
	
	
	private int setControlCommand(int reqType ,int request, int value, int index, byte[] data)
	{
		int dataLength = 0;
		if(data != null)
			dataLength = data.length;
		int response = connection.controlTransfer(reqType, request, value, index, data, dataLength, USB_TIMEOUT);
		Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
		return response;
	}


}
