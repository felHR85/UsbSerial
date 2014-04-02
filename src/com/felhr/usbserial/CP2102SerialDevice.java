package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

public class CP2102SerialDevice extends UsbSerialDevice
{
	private static final String CLASS_ID = CP2102SerialDevice.class.getSimpleName();
	
	private static final int CP210x_IFC_ENABLE = 0x00;
	private static final int CP210x_SET_BAUDDIV = 0x01;
	private static final int CP210x_SET_LINE_CTL = 0x03;
	private static final int CP210x_SET_MHS = 0x07;
	private static final int CP210x_SET_BAUDRATE = 0x1E;
	private static final int CP210x_SET_FLOW = 0x13;
	private static final int CP210x_SET_XON = 0x09;
	private static final int CP210x_SET_XOFF = 0x0A;
	
	private static final int CP210x_REQTYPE_HOST2DEVICE = 0x41;
	
	/***
	 *  Default Serial Configuration
	 *  Baud rate: 9600
	 *  Data bits: 8
	 *  Stop bits: 1
	 *  Parity: None
	 *  Flow Control: Off
	 */
	private static final int CP210x_UART_ENABLE = 0x0001;
	private static final int CP210x_UART_DISABLE = 0x0000;
	private static final int CP210x_LINE_CTL_DEFAULT = 0x0800;
	private static final int CP210x_MHS_DEFAULT = 0x0000;
	private static final int CP210x_MHS_DTR = 0x0001;
	private static final int CP210x_MHS_RTS = 0x0010;
	private static final int CP210x_MHS_ALL = 0x0011;
	private static final int CP210x_XON = 0x0000;
	private static final int CP210x_XOFF = 0x0000;
	private static final int USB_TIMEOUT = 5000;
	private static final int DEFAULT_BAUDRATE = 9600;
	
	private UsbInterface mInterface;
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;
	private UsbRequest requestIN;
	private UsbRequest requestOUT;

	public CP2102SerialDevice(UsbDevice device, UsbDeviceConnection connection) 
	{
		super(device, connection);
	}

	@Override
	public void open() 
	{
		// Get and claim interface
		mInterface = device.getInterface(0); // CP2102 has only one interface
		
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
		setControlCommand(CP210x_IFC_ENABLE, CP210x_UART_ENABLE, null);
		setBaudRate(DEFAULT_BAUDRATE);
		setControlCommand(CP210x_SET_LINE_CTL, CP210x_LINE_CTL_DEFAULT,null);
		setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
		setControlCommand(CP210x_SET_MHS, CP210x_MHS_DEFAULT, null);
		
		// Initialize UsbRequest
		requestIN = new UsbRequest();
		requestOUT = new UsbRequest();
		requestIN.initialize(connection, inEndpoint);
		requestOUT.initialize(connection, outEndpoint);
	}

	@Override
	public void write(byte[] buffer) 
	{
		serialBuffer.putWriteBuffer(buffer);
		requestOUT.queue(serialBuffer.getWriteBuffer(), buffer.length);
	}

	// This methods is going to need callback
	@Override
	public int read() 
	{
		// Input parameter, callback reference
		// callback reference needed to be passed to WorkingThread
		requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE); 
		return 0;
	}

	@Override
	public void close() 
	{
		// TODO 
	}

	@Override
	public void setBaudRate(int baudRate) 
	{
		byte[] data = new byte[] {
				(byte) (baudRate & 0xff),
				(byte) (baudRate >> 8 & 0xff),
				(byte) (baudRate >> 16 & 0xff),
				(byte) (baudRate >> 24 & 0xff)
		};
		setControlCommand(CP210x_SET_BAUDRATE, 0, data);
	}

	@Override
	public void setDataBits(int dataBits) 
	{
		// TODO 
	}

	@Override
	public void setStopBits(int stopBits) 
	{
		// TODO 
	}

	@Override
	public void setParity(int parity) 
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void setFlowControl(int flowControl) 
	{
		switch(flowControl)
		{
		case UsbSerialInterface.FLOW_CONTROL_OFF:
			byte[] data = new byte[]{
					0x00000000,
					0x00000000	
			};
			setControlCommand(CP210x_SET_FLOW, 0, data);
			break;
		case UsbSerialInterface.FLOW_CONTROL_RTS_CTS_IN:
			// TO-DO
			break;
		case UsbSerialInterface.FLOW_CONTROL_RTS_CTS_OUT:
			// TO-DO
			break;
		case UsbSerialInterface.FLOW_CONTROL_XON_XOFF_OUT:
			// TO-DO
			break;
		}
	}
	
	private int setControlCommand(int request, int value, byte[] data)
	{
		int dataLength = 0;
		if(data != null)
		{
			dataLength = data.length;
		}
		return connection.controlTransfer(CP210x_REQTYPE_HOST2DEVICE, request, value, 0, data, dataLength, USB_TIMEOUT);
	}

}
