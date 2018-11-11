package com.felhr.examplemultipleports;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.felhr.usbserial.SerialPortBuilder;
import com.felhr.usbserial.SerialPortCallback;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.List;

/**
 * Created by Felipe Herranz(felhr85@gmail.com) on 09/11/2018.
 */
public class UsbService extends Service {

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

    private List<UsbSerialDevice> serialPorts;

    private Context context;
    private UsbManager usbManager;
    private SerialPortBuilder builder;

    private IBinder binder = new UsbBinder();

    private Handler mHandler;

    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                boolean ret = builder.openSerialPorts(context, BAUD_RATE,
                        UsbSerialInterface.DATA_BITS_8,
                        UsbSerialInterface.STOP_BITS_1,
                        UsbSerialInterface.PARITY_NONE,
                        UsbSerialInterface.FLOW_CONTROL_OFF);
               if(ret)
                   Toast.makeText(context, "Couldnt open the device", Toast.LENGTH_SHORT).show();
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                UsbDevice usbDevice = arg1.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                List<UsbSerialDevice> filteredDevice = Stream.of(serialPorts)
                        .filter(p -> usbDevice.getDeviceId() == p.getDeviceId())
                        .toList();

                if(filteredDevice.size() == 1){
                    UsbSerialDevice disconnectedDevice = filteredDevice.get(0);
                    disconnectedDevice.syncClose();
                }

                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);

            }
        }
    };

    private final SerialPortCallback serialPortCallback = new SerialPortCallback() {
        @Override
        public void onSerialPortsDetected(List<UsbSerialDevice> serialPorts, boolean opened) {
            //TODO!!
        }
    };

    @Override
    public void onCreate() {
        this.context = this;
        UsbService.SERVICE_CONNECTED = true;
        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        builder = SerialPortBuilder.createSerialPortBuilder(serialPortCallback);

        boolean ret = builder.openSerialPorts(context, BAUD_RATE,
                UsbSerialInterface.DATA_BITS_8,
                UsbSerialInterface.STOP_BITS_1,
                UsbSerialInterface.PARITY_NONE,
                UsbSerialInterface.FLOW_CONTROL_OFF);

        if(!ret)
            Toast.makeText(context, "No Usb serial ports available", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }


    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(usbReceiver, filter);
    }

    public class UsbBinder extends Binder {
        public UsbService getService() {
            return UsbService.this;
        }
    }
}
