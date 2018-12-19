package com.felhr.usbserial;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import com.felhr.deviceids.CP2130Ids;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class UsbSpiDevice implements UsbSpiInterface
{
    private static final String CLASS_ID = UsbSerialDevice.class.getSimpleName();

    protected static final int USB_TIMEOUT = 5000;

    protected final UsbDevice device;
    protected final UsbDeviceConnection connection;

    protected SerialBuffer serialBuffer;

    protected WriteThread writeThread;
    protected ReadThread readThread;

    // Endpoints for synchronous read and write operations
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;

    public UsbSpiDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        this.device = device;
        this.connection = connection;
        this.serialBuffer = new SerialBuffer(false);
    }

    public static UsbSpiDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection)
    {
        return createUsbSerialDevice(device, connection, -1);
    }

    public static UsbSpiDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface)
    {
        int vid = device.getVendorId();
        int pid = device.getProductId();

        if(CP2130Ids.isDeviceSupported(vid, pid))
            return new CP2130SpiDevice(device, connection, iface);
        else
            return null;
    }


    @Override
    public abstract boolean connectSPI();

    @Override
    public abstract void writeMOSI(byte[] buffer);

    @Override
    public abstract void readMISO(int lengthBuffer);

    @Override
    public abstract void writeRead(byte[] buffer, int lengthRead);

    @Override
    public abstract void setClock(int clockDivider);

    @Override
    public abstract void selectSlave(int nSlave);

    @Override
    public void setMISOCallback(UsbMISOCallback misoCallback)
    {
        readThread.setCallback(misoCallback);
    }

    @Override
    public abstract int getClockDivider();

    @Override
    public abstract int getSelectedSlave();

    @Override
    public abstract void closeSPI();

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
                if(data.length > 0)
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
        private UsbMISOCallback misoCallback;
        private UsbEndpoint inEndpoint;
        private AtomicBoolean working;

        public ReadThread()
        {
            working = new AtomicBoolean(true);
        }

        public void setCallback(UsbMISOCallback misoCallback)
        {
            this.misoCallback = misoCallback;
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
                    onReceivedData(dataReceived);
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
            if(misoCallback != null)
                misoCallback.onReceivedData(data);
        }
    }

    protected void setThreadsParams(UsbEndpoint inEndpoint, UsbEndpoint outEndpoint)
    {
        if(writeThread != null)
            writeThread.setUsbEndpoint(outEndpoint);

        if(readThread != null)
            readThread.setUsbEndpoint(inEndpoint);
    }

    /*
     * Kill workingThread; This must be called when closing a device
     */
    protected void killWorkingThread()
    {
        if(readThread != null)
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
        readThread = new ReadThread();
        readThread.start();
        while(!readThread.isAlive()){} // Busy waiting
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
