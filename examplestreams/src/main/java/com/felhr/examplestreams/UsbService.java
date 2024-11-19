package com.felhr.examplestreams;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.SerialInputStream;
import com.felhr.usbserial.SerialOutputStream;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class UsbService extends Service {

    public static final String TAG = "UsbService";
    
    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;
    public static final int SYNC_READ = 3;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 9600; // BaudRate. Change this value if you need
    public static boolean SERVICE_CONNECTED = false;

    private IBinder binder = new UsbBinder();

    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private boolean serialPortConnected;

    private SerialInputStream serialInputStream;
    private SerialOutputStream serialOutputStream;

    private ReadThread readThread;

    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    connection = usbManager.openDevice(device);
                    new ConnectionThread().start();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            } else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                if (!serialPortConnected)
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected. send an intent to the Main Activity
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                if (serialPortConnected) {
                    serialPort.syncClose();
                    readThread.setKeep(false);
                }
                serialPortConnected = false;
            }
        }
    };

    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate() {
        this.context = this;
        serialPortConnected = false;
        UsbService.SERVICE_CONNECTED = true;
        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();
    }

    /* MUST READ about services
     * http://developer.android.com/guide/components/services.html
     * http://developer.android.com/guide/components/bound-services.html
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serialPort.close();
        unregisterReceiver(usbReceiver);
        UsbService.SERVICE_CONNECTED = false;
    }

    /*
     * This function will be called from MainActivity to write data through Serial Port
     */
    public void write(byte[] data) {
        if (serialOutputStream != null)
            serialOutputStream.write(data);
    }

    /*
     * This function will be called from MainActivity to change baud rate
     */

    public void changeBaudRate(int baudRate){
        if(serialPort != null)
            serialPort.setBaudRate(baudRate);
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            
            // first, dump the map for diagnostic purposes
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                Log.d(TAG, String.format("USBDevice.HashMap (vid:pid) (%X:%X)-%b class:%X:%X name:%s",
                        device.getVendorId(), device.getProductId(),
                        UsbSerialDevice.isSupported(device),
                        device.getDeviceClass(), device.getDeviceSubclass(),
                        device.getDeviceName()));
            }

            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

//                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                if (UsbSerialDevice.isSupported(device)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    break;
                } else {
                    connection = null;
                    device = null;
                }
            }
            if (device==null) {
                // There are no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        } else {
            Log.d(TAG, "findSerialPortDevice() usbManager returned empty device list." );
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(usbReceiver, filter);
    }

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission() {
        Log.d(TAG, String.format("requestUserPermission(%X:%X)", device.getVendorId(), device.getProductId() ) );
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        usbManager.requestPermission(device, mPendingIntent);
    }

    public class UsbBinder extends Binder {
        public UsbService getService() {
            return UsbService.this;
        }
    }

    /*
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.syncOpen()) {
                    serialPortConnected = true;
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    /**
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                    /**
                     * InputStream and OutputStream will be null if you are using async api.
                     */
                    serialInputStream = serialPort.getInputStream();
                    serialOutputStream = serialPort.getOutputStream();

                    readThread = new ReadThread();
                    readThread.start();

                    //
                    // Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going
                    // to be uploaded or not
                    //Thread.sleep(2000); // sleep some. YMMV with different chips.

                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_READY);
                    context.sendBroadcast(intent);
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort instanceof CDCSerialDevice) {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        context.sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        context.sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
            }
        }
    }

    private static String toASCII(int value) {
        int length = 4;
        StringBuilder builder = new StringBuilder(length);
        for (int i = length - 1; i >= 0; i--) {
            builder.append((char) ((value >> (8 * i)) & 0xFF));
        }
        return builder.toString();
    }

    private class ReadThread extends Thread {
        private AtomicBoolean keep = new AtomicBoolean(true);
        @Override
        public void run() {
            while(keep.get()){
                if(serialInputStream == null)
                    return;
                int value = serialInputStream.read();
                if(value != -1) {
                    String str = toASCII(value);
                    mHandler.obtainMessage(SYNC_READ, str).sendToTarget();
                }
            }
        }

        public void setKeep(boolean keep){
            this.keep.set(keep);
        }
    }
}
