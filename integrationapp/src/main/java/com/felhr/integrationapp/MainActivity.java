package com.felhr.integrationapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Set;

import static com.felhr.integrationapp.UsbService.MESSAGE_TEST_1;
import static com.felhr.integrationapp.UsbService.MESSAGE_TEST_2;
import static com.felhr.integrationapp.UsbService.MESSAGE_TEST_3;
import static com.felhr.integrationapp.UsbService.MESSAGE_TEST_4;
import static com.felhr.integrationapp.UsbService.MESSAGE_TEST_5;

public class MainActivity extends AppCompatActivity {

    private static final int MODE = 0; // 0: Async, 1: Sync, 2: Streams

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    statusText.setText("USB connected. Run Python tests");
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    resetTestTextViews();
                    statusText.setText("Connect USB Device");
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    resetTestTextViews();
                    statusText.setText("Connect USB Device");
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    resetTestTextViews();
                    statusText.setText("Connect USB Device");
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    resetTestTextViews();
                    statusText.setText("Connect USB Device");
                    break;
            }
        }
    };
    private UsbService usbService;
    private UsbSyncService usbSyncService;
    private TextView test1, test2, test3, test4, test5;
    private TextView statusText;
    private MyHandler mHandler;

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    private final ServiceConnection usbSyncConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbSyncService = ((UsbSyncService.UsbBinder) arg1).getService();
            usbSyncService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbSyncService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        test1 = findViewById(R.id.test_1_result);
        test2 = findViewById(R.id.test_2_result);
        test3 = findViewById(R.id.test_3_result);
        test4 = findViewById(R.id.test_4_result);
        test5 = findViewById(R.id.test_5_result);
        statusText = findViewById(R.id.status);
        mHandler = new MyHandler(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        Toast.makeText(this, "Mode: " + String.valueOf(MODE), Toast.LENGTH_LONG).show();
        if (MODE == 0) {
            startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
        }else if (MODE == 1) {
            startService(UsbSyncService.class, usbSyncConnection, null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        if(MODE == 0) {
            unbindService(usbConnection);
        }else{
            unbindService(usbSyncConnection);
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    private void resetTestTextViews(){
        test1.setText("TEST1: Not Passed");
        test2.setText("TEST2: Not Passed");
        test3.setText("TEST3: Not Passed");
        test4.setText("TEST4: Not Passed");
        test5.setText("TEST5: Not Passed");
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            String data = (String) msg.obj;
            switch (msg.what) {
                case MESSAGE_TEST_1:
                    mActivity.get().test1.setText(data);
                    break;

                case MESSAGE_TEST_2:
                    mActivity.get().test2.setText(data);
                    break;

                case MESSAGE_TEST_3:
                    mActivity.get().test3.setText(data);
                    break;

                case MESSAGE_TEST_4:
                    mActivity.get().test4.setText(data);
                    break;

                case MESSAGE_TEST_5:
                    mActivity.get().test5.setText(data);
                    break;
            }
        }
    }
}
