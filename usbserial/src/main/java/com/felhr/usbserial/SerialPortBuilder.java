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

public class SerialPortBuilder {
    private static final String ACTION_USB_PERMISSION = "com.felhr.usbserial.USB_PERMISSION";
    private static final int MODE_START = 0;
    private static final int MODE_OPEN = 1;

    private static SerialPortBuilder SerialPortBuilder;

    private List<UsbDeviceStatus> devices;
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
        return Stream.of(allDevices.values())
                .filter(UsbSerialDevice::isSupported)
                .toList();
    }

    public boolean getSerialPorts(Context context){

        if(devices == null) {
            devices = Stream.of(getPossibleSerialPorts())
                    .map(UsbDeviceStatus::new)
                    .toList();
        }else{
            devices.addAll(Stream.of(getPossibleSerialPorts())
                    .map(UsbDeviceStatus::new)
                    .filter(p -> !devices.contains(p))
                    .toList());
        }

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

    public boolean disconnectDevice(UsbSerialDevice usbSerialDevice){
        usbSerialDevice.syncClose();
        serialDevices = Utils.removeIf(serialDevices, p -> usbSerialDevice.getDeviceId() == p.getDeviceId());
        return true;
    }

    public boolean disconnectDevice(UsbDevice usbDevice){
        Optional<UsbSerialDevice> optionalDevice = Stream.of(serialDevices)
                .filter(p -> usbDevice.getDeviceId() == p.getDeviceId())
                .findSingle();

        if(optionalDevice.isPresent()){
            UsbSerialDevice disconnectedDevice = optionalDevice.get();
            disconnectedDevice.syncClose();
            serialDevices = Utils.removeIf(serialDevices, p -> usbDevice.getDeviceId() == p.getDeviceId());
            return true;
        }
        return false;
    }

    private boolean requestPermission(Context context){
        if(!devices.get(index).open) {
            PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(devices.get(index).usbDevice, mPendingIntent);
            return true;
        }else{
            index++;
            if(index <= devices.size()-1)
                return requestPermission(context);
            else
                return false;
        }
    }

    private void initReceiver(Context context){
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        context.registerReceiver(usbReceiver, filter);
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
                if (granted) {
                    devices.get(index).open = true;
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
            serialPortCallback.onSerialPortsDetected(serialDevices, true);
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
            UsbDevice objDevice = (UsbDevice) obj;
            return (objDevice.getVendorId() ==  usbDevice.getVendorId())
                    && (objDevice.getProductId() == usbDevice.getProductId());

        }
    }
}
