package com.felhr.usbserial;

import java.util.concurrent.atomic.AtomicBoolean;

import com.felhr.deviceids.CH34xIds;
import com.felhr.deviceids.CP210xIds;
import com.felhr.deviceids.FTDISioIds;
import com.felhr.deviceids.PL2303Ids;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;

public abstract class UsbSerialDevice implements UsbSerialInterface
{
    private static final String CLASS_ID = UsbSerialDevice.class.getSimpleName();

    private static boolean mr1Version;
    protected final UsbDevice device;
    protected final UsbDeviceConnection connection;

    protected static final int USB_TIMEOUT = 5000;

    protected SerialBuffer serialBuffer;

    protected WorkerThread workerThread;
    protected WriteThread writeThread;
    protected ReadThread readThread;

    // Endpoints for synchronous read and write operations
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;

    protected boolean asyncMode;

    // Get Android version if version < 4.3 It is not going to be asynchronous read operations
    static
    {
        if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
            mr1Version = true;
        else
            mr1Version = false;
    }

    public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        this.device = device;
        this.connection = connection;
        this.asyncMode = true;
        serialBuffer = new SerialBuffer(mr1Version);
    }

    public static UsbSerialDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        return createUsbSerialDevice(device, connection, -1);
    }

    public static UsbSerialDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface)
    {
		/*
		 * It checks given vid and pid and will return a custom driver or a CDC serial driver.
		 * When CDC is returned open() method is even more important, its response will inform about if it can be really
		 * opened as a serial device with a generic CDC serial driver
		 */
        int vid = device.getVendorId();
        int pid = device.getProductId();

        if(FTDISioIds.isDeviceSupported(vid, pid))
            return new FTDISerialDevice(device, connection, iface);
        else if(CP210xIds.isDeviceSupported(vid, pid))
            return new CP2102SerialDevice(device, connection, iface);
        else if(PL2303Ids.isDeviceSupported(vid, pid))
            return new PL2303SerialDevice(device, connection, iface);
        else if(CH34xIds.isDeviceSupported(vid, pid))
            return new CH34xSerialDevice(device, connection, iface);
        else if(isCdcDevice(device))
            return new CDCSerialDevice(device, connection, iface);
        else
            return null;
    }

    // Common Usb Serial Operations (I/O Asynchronous)
    @Override
    public abstract boolean open();

    @Override
    public void write(byte[] buffer)
    {
        if(asyncMode)
            serialBuffer.putWriteBuffer(buffer);
    }

    @Override
    public int read(UsbReadCallback mCallback)
    {
        if(!asyncMode)
            return -1;

        if(mr1Version)
        {
            workerThread.setCallback(mCallback);
            workerThread.getUsbRequest().queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
        }else
        {
            readThread.setCallback(mCallback);
            //readThread.start();
        }
        return 0;
    }


    @Override
    public abstract void close();

    // Common Usb Serial Operations (I/O Synchronous)
    @Override
    public abstract boolean syncOpen();

    @Override
    public abstract void syncClose();

    @Override
    public int syncWrite(byte[] buffer, int timeout)
    {
        if(!asyncMode)
        {
            if(buffer == null)
                return 0;

            return connection.bulkTransfer(outEndpoint, buffer, buffer.length, timeout);
        }else
        {
            return -1;
        }
    }

    @Override
    public int syncRead(byte[] buffer, int timeout)
    {
        if(asyncMode)
        {
            return -1;
        }

        if (buffer == null)
            return 0;

        return connection.bulkTransfer(inEndpoint, buffer, buffer.length, timeout);
    }

    // Serial port configuration
    @Override
    public abstract void setBaudRate(int baudRate);
    @Override
    public abstract void setDataBits(int dataBits);
    @Override
    public abstract void setStopBits(int stopBits);
    @Override
    public abstract void setParity(int parity);
    @Override
    public abstract void setFlowControl(int flowControl);

    //Debug options
    public void debug(boolean value)
    {
        if(serialBuffer != null)
            serialBuffer.debug(value);
    }

    private boolean isFTDIDevice()
    {
        return (this instanceof FTDISerialDevice);
    }

    public static boolean isCdcDevice(UsbDevice device)
    {
        int iIndex = device.getInterfaceCount();
        for(int i=0;i<=iIndex-1;i++)
        {
            UsbInterface iface = device.getInterface(i);
            if(iface.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA)
                return true;
        }
        return false;
    }


    /*
     * WorkerThread waits for request notifications from IN endpoint
     */
    protected class WorkerThread extends Thread
    {
        private UsbSerialDevice usbSerialDevice;

        private UsbReadCallback callback;
        private UsbRequest requestIN;
        private AtomicBoolean working;

        public WorkerThread(UsbSerialDevice usbSerialDevice)
        {
            this.usbSerialDevice = usbSerialDevice;
            working = new AtomicBoolean(true);
        }

        @Override
        public void run()
        {
            while(working.get())
            {
                UsbRequest request = connection.requestWait();
                if(request != null && request.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                        && request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN)
                {
                    byte[] data = serialBuffer.getDataReceived();

                    // FTDI devices reserves two first bytes of an IN endpoint with info about
                    // modem and Line.
                    if(isFTDIDevice())
                    {
                        ((FTDISerialDevice) usbSerialDevice).ftdiUtilities.checkModemStatus(data); //Check the Modem status
                        serialBuffer.clearReadBuffer();

                        if(data.length > 2)
                        {
                            data = ((FTDISerialDevice) usbSerialDevice).ftdiUtilities.adaptArray(data);
                            onReceivedData(data);
                        }
                    }else
                    {
                        // Clear buffer, execute the callback
                        serialBuffer.clearReadBuffer();
                        onReceivedData(data);
                    }
                    // Queue a new request
                    requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
                }
            }
        }

        public void setCallback(UsbReadCallback callback)
        {
            this.callback = callback;
        }

        public void setUsbRequest(UsbRequest request)
        {
            this.requestIN = request;
        }

        public UsbRequest getUsbRequest()
        {
            return requestIN;
        }

        private void onReceivedData(byte[] data)
        {
            if(callback != null)
                callback.onReceivedData(data);
        }

        public void stopWorkingThread()
        {
            working.set(false);
        }
    }

    protected class WriteThread extends Thread
    {
        private UsbEndpoint outEndpoint;
        private AtomicBoolean working;

        public WriteThread()
        {
            working = new AtomicBoolean(true);
        }

        @Override
        public void run()
        {
            while(working.get())
            {
                byte[] data = serialBuffer.getWriteBuffer();
                connection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);
            }
        }

        public void setUsbEndpoint(UsbEndpoint outEndpoint)
        {
            this.outEndpoint = outEndpoint;
        }

        public void stopWriteThread()
        {
            working.set(false);
        }
    }

    protected class ReadThread extends Thread
    {
        private UsbSerialDevice usbSerialDevice;

        private UsbReadCallback callback;
        private UsbEndpoint inEndpoint;
        private AtomicBoolean working;

        public ReadThread(UsbSerialDevice usbSerialDevice)
        {
            this.usbSerialDevice = usbSerialDevice;
            working = new AtomicBoolean(true);
        }

        public void setCallback(UsbReadCallback callback)
        {
            this.callback = callback;
        }

        @Override
        public void run()
        {
            byte[] dataReceived = null;

            while(working.get())
            {
                int numberBytes;
                if(inEndpoint != null)
                    numberBytes = connection.bulkTransfer(inEndpoint, serialBuffer.getBufferCompatible(),
                            SerialBuffer.DEFAULT_READ_BUFFER_SIZE, 0);
                else
                    numberBytes = 0;

                if(numberBytes > 0)
                {
                    dataReceived = serialBuffer.getDataReceivedCompatible(numberBytes);

                    // FTDI devices reserve two first bytes of an IN endpoint with info about
                    // modem and Line.
                    if(isFTDIDevice())
                    {
                        ((FTDISerialDevice) usbSerialDevice).ftdiUtilities.checkModemStatus(dataReceived);

                        if(dataReceived.length > 2)
                        {
                            dataReceived = ((FTDISerialDevice) usbSerialDevice).ftdiUtilities.adaptArray(dataReceived);
                            onReceivedData(dataReceived);
                        }
                    }else
                    {
                        onReceivedData(dataReceived);
                    }
                }
            }
        }

        public void setUsbEndpoint(UsbEndpoint inEndpoint)
        {
            this.inEndpoint = inEndpoint;
        }

        public void stopReadThread()
        {
            working.set(false);
        }

        private void onReceivedData(byte[] data)
        {
            if(callback != null)
                callback.onReceivedData(data);
        }
    }

    protected void setSyncParams(UsbEndpoint inEndpoint, UsbEndpoint outEndpoint)
    {
        this.inEndpoint = inEndpoint;
        this.outEndpoint = outEndpoint;
    }

    protected void setThreadsParams(UsbRequest request, UsbEndpoint endpoint)
    {
        if(mr1Version)
        {
            workerThread.setUsbRequest(request);
            writeThread.setUsbEndpoint(endpoint);
        }else
        {
            readThread.setUsbEndpoint(request.getEndpoint());
            writeThread.setUsbEndpoint(endpoint);
        }
    }

    /*
     * Kill workingThread; This must be called when closing a device
     */
    protected void killWorkingThread()
    {
        if(mr1Version && workerThread != null)
        {
            workerThread.stopWorkingThread();
            workerThread = null;
        }else if(!mr1Version && readThread != null)
        {
            readThread.stopReadThread();
            readThread = null;
        }
    }

    /*
     * Restart workingThread if it has been killed before
     */
    protected void restartWorkingThread()
    {
        if(mr1Version && workerThread == null)
        {
            workerThread = new WorkerThread(this);
            workerThread.start();
            while(!workerThread.isAlive()){} // Busy waiting
        }else if(!mr1Version && readThread == null)
        {
            readThread = new ReadThread(this);
            readThread.start();
            while(!readThread.isAlive()){} // Busy waiting
        }
    }

    protected void killWriteThread()
    {
        if(writeThread != null)
        {
            writeThread.stopWriteThread();
            writeThread = null;
            serialBuffer.resetWriteBuffer();
        }
    }

    protected void restartWriteThread()
    {
        if(writeThread == null)
        {
            writeThread = new WriteThread();
            writeThread.start();
            while(!writeThread.isAlive()){} // Busy waiting
        }
    }
}