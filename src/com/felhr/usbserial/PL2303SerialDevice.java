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
	
	private static final int PL2303_REQTYPE_HOST2DEVICE = 0x40;
	private static final int PL2303_REQTYPE_DEVICE2HOST = 0xC0;
	
	private static final int PL2303_VENDOR_WRITE_REQUEST = 0x01;
	
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
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_VENDOR_WRITE_REQUEST, 0x0404, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST, PL2303_VENDOR_WRITE_REQUEST, 0x8383, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_VENDOR_WRITE_REQUEST, 0x0404, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST, PL2303_VENDOR_WRITE_REQUEST, 0x8484, 0, buf);
		setControlCommand(PL2303_REQTYPE_DEVICE2HOST, PL2303_VENDOR_WRITE_REQUEST, 0x8383, 0, buf);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_VENDOR_WRITE_REQUEST, 0x0000, 0, null);
		setControlCommand(PL2303_REQTYPE_HOST2DEVICE, PL2303_VENDOR_WRITE_REQUEST, 0x0001, 0, null);
		
		
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBaudRate(int baudRate) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDataBits(int dataBits) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStopBits(int stopBits)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setParity(int parity) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFlowControl(int flowControl)
	{
		// TODO Auto-generated method stub
		
	}
	
	private int setControlCommand(int reqType ,int request, int value, int index, byte[] data)
	{
		int dataLength = 0;
		if(data != null)
		{
			dataLength = data.length;
		}
		int response = connection.controlTransfer(reqType, request, value, index, data, dataLength, USB_TIMEOUT);
		Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
		return response;
	}


}
