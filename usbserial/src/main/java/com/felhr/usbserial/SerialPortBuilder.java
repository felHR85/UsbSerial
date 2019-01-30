package com.felhr.usbserial;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.felhr.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class SerialPortBuilder {
    private static final String ACTION_USB_PERMISSION = "com.felhr.usbserial.USB_PERMISSION";
    private static final int MODE_START = 0;
    private static final int MODE_OPEN = 1;

    private static SerialPortBuilder SerialPortBuilder;

    private List<UsbDeviceStatus> devices;
    private List<UsbSerialDevice> serialDevices = new ArrayList<>();

    private final ArrayBlockingQueue<PendingUsbPermission> queuedPermissions = new ArrayBlockingQueue<>(100);
    private volatile boolean processingPermission = false;
    private PendingUsbPermission currentPendingPermission;

    private UsbManager usbManager;
    private final SerialPortCallback serialPortCallback;

    private int baudRate, dataBits, stopBits, parity, flowControl;
    private int mode = 0;

    private boolean broadcastRegistered = false;

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


    public List<UsbDevice> getPossibleSerialPorts(Context context){

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> allDevices = usbManager.getDeviceList();
        List<UsbDevice> devices =  Stream.of(allDevices.values())
                .filter(UsbSerialDevice::isSupported)
                .toList();

        return devices;
    }

    public boolean getSerialPorts(Context context){

        initReceiver(context);

        if(devices == null || devices.size() == 0) { // Not previous devices detected
            devices = Stream.of(getPossibleSerialPorts(context))
                    .map(UsbDeviceStatus::new)
                    .toList();

            if(devices.size() == 0)
                return false;

            for(UsbDeviceStatus deviceStatus : devices){
                queuedPermissions.add(createUsbPermission(context, deviceStatus));
            }

            if(!processingPermission){
                launchPermission();
            }

        }else{ // Previous devices detected and maybe pending permissions intent launched

            List<UsbDeviceStatus> newDevices = Stream.of(getPossibleSerialPorts(context))
                    .map(UsbDeviceStatus::new)
                    .filter(p -> !devices.contains(p))
                    .toList();

            if(newDevices.size() == 0)
                return false;

            for(UsbDeviceStatus deviceStatus : newDevices){
                queuedPermissions.add(createUsbPermission(context, deviceStatus));
            }

            devices.addAll(newDevices);

            if(!processingPermission){
                launchPermission();
            }
        }

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

    public boolean disconnectDevice(UsbSerialDevice usbSerialDevice){
        usbSerialDevice.syncClose();
        serialDevices = Utils.removeIf(serialDevices, p -> usbSerialDevice.getDeviceId() == p.getDeviceId());
        return true;
    }

    public boolean disconnectDevice(UsbDevice usbDevice){
        List<UsbSerialDevice> devices = Stream.of(serialDevices)
                .filter(p -> usbDevice.getDeviceId() == p.getDeviceId())
                .toList();

        int removedDevices = 0;
        for(UsbSerialDevice device : devices){
            device.syncClose();
            serialDevices = Utils.removeIf(serialDevices, p -> usbDevice.getDeviceId() == p.getDeviceId());
            removedDevices ++;
        }

        return removedDevices == devices.size();
    }

    public void unregisterListeners(Context context){
        if(broadcastRegistered){
            context.unregisterReceiver(usbReceiver);
            broadcastRegistered = false;
        }
    }

    private PendingUsbPermission createUsbPermission(Context context, UsbDeviceStatus usbDeviceStatus){
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        PendingUsbPermission pendingUsbPermission = new PendingUsbPermission();
        pendingUsbPermission.pendingIntent = mPendingIntent;
        pendingUsbPermission.usbDeviceStatus = usbDeviceStatus;
        return pendingUsbPermission;
    }


    private void launchPermission(){
        try {
            processingPermission = true;
            currentPendingPermission = queuedPermissions.take();
            usbManager.requestPermission(currentPendingPermission.usbDeviceStatus.usbDevice,
                    currentPendingPermission.pendingIntent);
        } catch (InterruptedException e) {
            e.printStackTrace();
            processingPermission = false;
        }
    }

    private void initReceiver(Context context){
        if(!broadcastRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_USB_PERMISSION);
            context.registerReceiver(usbReceiver, filter);
            broadcastRegistered = true;
        }
    }

    private void createAllPorts(UsbDeviceStatus usbDeviceStatus){
        int interfaceCount = usbDeviceStatus.usbDevice.getInterfaceCount();
        for(int i=0;i<=interfaceCount-1;i++) {
            if(usbDeviceStatus.usbDeviceConnection == null) {
                usbDeviceStatus.usbDeviceConnection = usbManager.openDevice(usbDeviceStatus.usbDevice);
            }

            UsbSerialDevice usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(
                    usbDeviceStatus.usbDevice,
                    usbDeviceStatus.usbDeviceConnection,
                    i);

            serialDevices.add(usbSerialDevice);
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                InitSerialPortThread initSerialPortThread;
                if (granted) {
                    createAllPorts(currentPendingPermission.usbDeviceStatus);
                    if(queuedPermissions.size() > 0) {
                        launchPermission();
                    }else{
                        processingPermission = false;
                        if(mode == MODE_START) {
                            serialPortCallback.onSerialPortsDetected(serialDevices);
                        }else{
                            initSerialPortThread = new InitSerialPortThread(serialDevices);
                            initSerialPortThread.start();
                        }
                    }
                } else {
                    if(queuedPermissions.size() > 0) {
                        launchPermission();
                    }else{
                        processingPermission = false;
                        if(mode == MODE_START) {
                            serialPortCallback.onSerialPortsDetected(serialDevices);
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

        private final List<UsbSerialDevice> usbSerialDevices;

        public InitSerialPortThread(List<UsbSerialDevice> usbSerialDevices) {
            this.usbSerialDevices = usbSerialDevices;
        }

        @Override
        public void run() {
            int n =1;
            for (UsbSerialDevice usbSerialDevice : usbSerialDevices) {
                if (!usbSerialDevice.isOpen) {
                    if (usbSerialDevice.syncOpen()) {
                        usbSerialDevice.setBaudRate(baudRate);
                        usbSerialDevice.setDataBits(dataBits);
                        usbSerialDevice.setStopBits(stopBits);
                        usbSerialDevice.setParity(parity);
                        usbSerialDevice.setFlowControl(flowControl);
                        usbSerialDevice.setPortName(UsbSerialDevice.COM_PORT + String.valueOf(n));
                        n++;
                    }
                }
            }
            serialPortCallback.onSerialPortsDetected(serialDevices);
        }
    }

    private class UsbDeviceStatus {
        public UsbDevice usbDevice;
        public UsbDeviceConnection usbDeviceConnection;
        public boolean open;

        public UsbDeviceStatus(UsbDevice usbDevice) {
            this.usbDevice = usbDevice;
        }

        @Override
        public boolean equals(Object obj) {
            UsbDeviceStatus usbDeviceStatus= (UsbDeviceStatus) obj;
            return usbDeviceStatus.usbDevice.getDeviceId() == usbDevice.getDeviceId();
        }
    }

    private class PendingUsbPermission{
        public PendingIntent pendingIntent;
        public UsbDeviceStatus usbDeviceStatus;
    }
}
