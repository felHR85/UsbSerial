package com.felhr.usbserial;

import java.util.Arrays;

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
	
	private int currentSioSetData = 0x0000;
	
	private UsbInterface mInterface;
	private UsbEndpoint inEndpoint;
	private UsbEndpoint outEndpoint;
	private UsbRequest requestIN;
	
	
	public FTDISerialDevice(UsbDevice device, UsbDeviceConnection connection) 
	{
		this(device, connection, -1);
	}

	public FTDISerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface)
	{
		super(device, connection);
		mInterface = device.getInterface(iface >= 0 ? iface : 0);
	}

	@Override
	public boolean open() 
	{
		// Restart the working thread and writeThread if it has been killed before and claim interface
		restartWorkingThread();
		restartWriteThread();

		if(connection.claimInterface(mInterface, true))
		{
			Log.i(CLASS_ID, "Interface succesfully claimed");
		}else
		{
			Log.i(CLASS_ID, "Interface could not be claimed");
			return false;
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
		if(setControlCommand(FTDI_SIO_RESET, 0x00, 0, null) < 0)
			return false;
		if(setControlCommand(FTDI_SIO_SET_DATA, FTDI_SET_DATA_DEFAULT, 0, null) < 0)
			return false;
		currentSioSetData = FTDI_SET_DATA_DEFAULT;
		if(setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT1, 0, null) < 0)
			return false;
		if(setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT2, 0, null) < 0)
			return false;
		if(setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, 0, null) < 0)
			return false;
		if(setControlCommand(FTDI_SIO_SET_BAUD_RATE, FTDI_BAUDRATE_9600, 0, null) < 0)
			return false;

		// Initialize UsbRequest
		requestIN = new UsbRequest();
		requestIN.initialize(connection, inEndpoint);

		// Pass references to the threads
		setThreadsParams(requestIN, outEndpoint);
		
		return true;
	}

	@Override
	public void close() 
	{
		setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT3, 0, null);
		setControlCommand(FTDI_SIO_MODEM_CTRL, FTDI_SET_MODEM_CTRL_DEFAULT4, 0, null);
		currentSioSetData = 0x0000;
		killWorkingThread();
		killWriteThread();
		connection.releaseInterface(mInterface);
	}

	@Override
	public void setBaudRate(int baudRate) 
	{
		int value = 0;
		if(baudRate >= 0 && baudRate <= 300 )
			value = FTDI_BAUDRATE_300;
		else if(baudRate > 300 && baudRate <= 600)
			value = FTDI_BAUDRATE_600;
		else if(baudRate > 600 && baudRate <= 1200)
			value = FTDI_BAUDRATE_1200;
		else if(baudRate > 1200 && baudRate <= 2400)
			value = FTDI_BAUDRATE_2400;
		else if(baudRate > 2400 && baudRate <= 4800)
			value = FTDI_BAUDRATE_4800;
		else if(baudRate > 4800 && baudRate <= 9600)
			value = FTDI_BAUDRATE_9600;
		else if(baudRate > 9600 && baudRate <=19200)
			value = FTDI_BAUDRATE_19200;
		else if(baudRate > 19200 && baudRate <= 38400)
			value = FTDI_BAUDRATE_38400;
		else if(baudRate > 19200 && baudRate <= 57600)
			value = FTDI_BAUDRATE_57600;
		else if(baudRate > 57600 && baudRate <= 115200)
			value = FTDI_BAUDRATE_115200;
		else if(baudRate > 115200 && baudRate <= 230400)
			value = FTDI_BAUDRATE_230400;
		else if(baudRate > 230400 && baudRate <= 460800)
			value = FTDI_BAUDRATE_460800;
		else if(baudRate > 460800 && baudRate <= 921600)
			value = FTDI_BAUDRATE_921600;
		else if(baudRate > 921600)
			value = FTDI_BAUDRATE_921600;
		else
			value = FTDI_BAUDRATE_9600;
		setControlCommand(FTDI_SIO_SET_BAUD_RATE, value, 0, null);	
	}

	@Override
	public void setDataBits(int dataBits)
	{
		switch(dataBits)
		{
		case UsbSerialInterface.DATA_BITS_5:
			currentSioSetData |= 1;
			currentSioSetData &= ~(1 << 1);
			currentSioSetData |= (1 << 2);
			currentSioSetData &= ~(1 << 3);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		case UsbSerialInterface.DATA_BITS_6:
			currentSioSetData &= ~1;
			currentSioSetData |= (1 << 1);
			currentSioSetData |= (1 << 2);
			currentSioSetData &= ~(1 << 3);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		case UsbSerialInterface.DATA_BITS_7:
			currentSioSetData |= 1;
			currentSioSetData |= (1 << 1);
			currentSioSetData |= (1 << 2);
			currentSioSetData &= ~(1 << 3);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		case UsbSerialInterface.DATA_BITS_8:
			currentSioSetData &= ~1;
			currentSioSetData &= ~(1 << 1);
			currentSioSetData &= ~(1 << 2);
			currentSioSetData |= (1 << 3);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		default:
			currentSioSetData &= ~1;
			currentSioSetData &= ~(1 << 1);
			currentSioSetData &= ~(1 << 2);
			currentSioSetData |= (1 << 3);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		}
		
	}

	@Override
	public void setStopBits(int stopBits) 
	{
		switch(stopBits)
		{
		case UsbSerialInterface.STOP_BITS_1:
			currentSioSetData &= ~(1 << 11);
			currentSioSetData &= ~(1 << 12);
			currentSioSetData &= ~(1 << 13);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		case UsbSerialInterface.STOP_BITS_15:
			currentSioSetData |= (1 << 11);
			currentSioSetData &= ~(1 << 12);
			currentSioSetData &= ~(1 << 13);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		case UsbSerialInterface.STOP_BITS_2:
			currentSioSetData &= ~(1 << 11);
			currentSioSetData |= (1 << 12);
			currentSioSetData &= ~(1 << 13);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		default:
			currentSioSetData &= ~(1 << 11);
			currentSioSetData &= ~(1 << 12);
			currentSioSetData &= ~(1 << 13);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);	
		}
		
	}

	@Override
	public void setParity(int parity) 
	{
		switch(parity)
		{
		case UsbSerialInterface.PARITY_NONE:
			currentSioSetData &= ~(1 << 8);
			currentSioSetData &= ~(1 << 9);
			currentSioSetData &= ~(1 << 10);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		case UsbSerialInterface.PARITY_ODD:
			currentSioSetData |= (1 << 8);
			currentSioSetData &= ~(1 << 9);
			currentSioSetData &= ~(1 << 10);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		case UsbSerialInterface.PARITY_EVEN:
			currentSioSetData &= ~(1 << 8);
			currentSioSetData |= (1 << 9);
			currentSioSetData &= ~(1 << 10);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		case UsbSerialInterface.PARITY_MARK:
			currentSioSetData |= (1 << 8);
			currentSioSetData |= (1 << 9);
			currentSioSetData &= ~(1 << 10);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		case UsbSerialInterface.PARITY_SPACE:
			currentSioSetData &= ~(1 << 8);
			currentSioSetData &= ~(1 << 9);
			currentSioSetData |= (1 << 10);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		default:
			currentSioSetData &= ~(1 << 8);
			currentSioSetData &= ~(1 << 9);
			currentSioSetData &= ~(1 << 10);
			setControlCommand(FTDI_SIO_SET_DATA, currentSioSetData, 0, null);
			break;
		}
		
	}

	@Override
	public void setFlowControl(int flowControl) 
	{
		switch(flowControl)
		{
		case UsbSerialInterface.FLOW_CONTROL_OFF:
			setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, 0, null);
			break;
		case UsbSerialInterface.FLOW_CONTROL_RTS_CTS:
			int indexRTSCTS = 0x0001;
			setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, indexRTSCTS, null);
			break;
		case UsbSerialInterface.FLOW_CONTROL_DSR_DTR:
			int indexDSRDTR = 0x0002;
			setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, indexDSRDTR , null);
			break;
		case UsbSerialInterface.FLOW_CONTROL_XON_XOFF:
			int indexXONXOFF = 0x0004;
			int wValue = 0x1311;
			setControlCommand(FTDI_SIO_SET_FLOW_CTRL, wValue, indexXONXOFF , null);
			break;
		default:
			setControlCommand(FTDI_SIO_SET_FLOW_CTRL, FTDI_SET_FLOW_CTRL_DEFAULT, 0, null);
			break;
		}
	}
	
	private int setControlCommand(int request, int value, int index, byte[] data)
	{
		int dataLength = 0;
		if(data != null)
		{
			dataLength = data.length;
		}
		int response = connection.controlTransfer(FTDI_REQTYPE_HOST2DEVICE, request, value, mInterface.getId() + 1 + index, data, dataLength, USB_TIMEOUT);
		Log.i(CLASS_ID,"Control Transfer Response: " + String.valueOf(response));
		return response;
	}
	
	public static class FTDIUtilities
	{
		// Special treatment needed to FTDI devices
		public static byte[] adaptArray(byte[] ftdiData)
		{
			int length = ftdiData.length;
			if(length > 64)
			{
				int n = 1;
				int p = 64;
				// Precalculate length without FTDI headers
				while(p < length)
				{
					n++;
					p = n*64;
				}
				int realLength = length - n*2;
				byte[] data = new byte[realLength];
				copyData(ftdiData, data);
				return data;
			}else
			{
				return Arrays.copyOfRange(ftdiData, 2, length);
			}	
		}
		
		// Copy data without FTDI headers
		private static void copyData(byte[] src, byte[] dst)
		{
			int i = 0; // src index
			int j = 0; // dst index
			while(i <= src.length-1)
			{
				if(i != 0 && i != 1)
				{
					if(i % 64 == 0 && i >= 64)
					{ 
						i += 2;
					}else
					{
						dst[j] = src[i];
						i++;
						j++;
					}	
				}else
				{
					i++;
				}
			}
		}
	}

}
