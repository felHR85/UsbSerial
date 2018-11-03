package com.felhr.usbserial;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SerialPortBuilder {
    private static final String ACTION_USB_PERMISSION = "com.felhr.usbserial.USB_PERMISSION";
    private static final int MODE_START = 0;
    private static final int MODE_OPEN = 1;

    private static SerialPortBuilder SerialPortBuilder;

    private List<UsbDevice> devices;
    private List<UsbSerialDevice> serialDevices = new ArrayList<>();

    private int index = 0;

    private UsbManager usbManager;
    private SerialPortCallback serialPortCallback;

    private InitSerialPortThread initSerialPortThread;

    private int baudRate, dataBits, stopBits, parity, flowControl;
    private int mode = 0;

    private SerialPortBuilder(SerialPortCallback serialPortCallback){
        this.serialPortCallback = serialPortCallback;
    }


    public static SerialPortBuilder createSerialPortBuilder(SerialPortCallback serialPortCallback){
        if(SerialPortBuilder == null) {
            SerialPortBuilder = new SerialPortBuilder(serialPortCallback);
            return SerialPortBuilder;
        }else {
            return SerialPortBuilder;
        }
    }


    public List<UsbDevice> getPossibleSerialPorts(){
        HashMap<String, UsbDevice> allDevices = usbManager.getDeviceList();

        return Stream.of(allDevices.values()).filter(
                p -> UsbSerialDevice.isSupported(p))
                .toList();
    }

    public boolean getSerialPorts(Context context){
        devices = getPossibleSerialPorts();

        if(devices == null || devices.size() == 0)
            return false;

        initReceiver(context);

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        requestPermission(context);

        return true;
    }

    public boolean openSerialPorts(Context context, int baudRate, int dataBits,
            int stopBits, int parity, int flowControl){
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.flowControl = flowControl;
        this.mode = MODE_OPEN;
        return getSerialPorts(context);
    }

    private void requestPermission(Context context){
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(devices.get(index), mPendingIntent);
    }

    private void initReceiver(Context context){
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        context.registerReceiver(usbReceiver, filter);
    }

    private void createAllPorts(UsbDevice usbDevice){
        int interfaceCount = usbDevice.getInterfaceCount();
        for(int i=0;i<=interfaceCount-1;i++) {
            //TODO: Maybe opening the same device more than once causes troubles!!!
            UsbSerialDevice usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(usbDevice,
                    usbManager.openDevice(usbDevice),
                    i);

            serialDevices.add(usbSerialDevice);
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    createAllPorts(devices.get(index));
                    index++;
                    if(index < devices.size()){
                        requestPermission(context);
                    }else{
                        if(mode == MODE_START) {
                            serialPortCallback.onSerialPortsDetected(serialDevices, false);
                        }else{
                            initSerialPortThread = new InitSerialPortThread(serialDevices);
                            initSerialPortThread.start();
                        }
                    }
                } else {
                    index++;
                    if(index < devices.size()){
                        requestPermission(context);
                    }else{
                        if(mode == MODE_START) {
                            serialPortCallback.onSerialPortsDetected(serialDevices, false);
                        }else{
                            initSerialPortThread = new InitSerialPortThread(serialDevices);
                            initSerialPortThread.start();
                        }
                    }
                }
            }
        }
    };

    private class InitSerialPortThread extends Thread {

        private List<UsbSerialDevice> usbSerialDevices;

        public InitSerialPortThread(List<UsbSerialDevice> usbSerialDevices) {
            this.usbSerialDevices = usbSerialDevices;
        }

        @Override
        public void run() {
            for (UsbSerialDevice usbSerialDevice : usbSerialDevices) {
                if (usbSerialDevice.syncOpen()) {
                    usbSerialDevice.setBaudRate(baudRate);
                    usbSerialDevice.setDataBits(dataBits);
                    usbSerialDevice.setStopBits(stopBits);
                    usbSerialDevice.setParity(parity);
                    usbSerialDevice.setFlowControl(flowControl);
                }
            }
            serialPortCallback.onSerialPortsDetected(serialDevices, true);
        }
    }
}
