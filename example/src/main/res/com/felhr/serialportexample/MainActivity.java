package com.felhr.serialportexample;

import java.lang.ref.WeakReference;
import java.util.Set;

import android.support.v7.app.ActionBarActivity;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements View.OnClickListener
{
	private UsbService usbService;
	
	private TextView display;
	private EditText editText;
	private Button sendButton;
	
	private MyHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mHandler = new MyHandler(this);
        
        display = (TextView) findViewById(R.id.textView1);
        editText = (EditText) findViewById(R.id.editText1);
        sendButton = (Button) findViewById(R.id.buttonSend);
        sendButton.setOnClickListener(this);
    }
    
    @Override
	public void onResume()
	{
    	super.onResume();
    	setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
	}
    
    @Override
    public void onPause()
    {
    	super.onPause();
    	unregisterReceiver(mUsbReceiver);
    	unbindService(usbConnection);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) 
        {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


	@Override
	public void onClick(View v) 
	{
		if(!editText.getText().toString().equals(""))
		{
			String data = editText.getText().toString();
			if(usbService != null) // if UsbService was correctly binded, Send data
				usbService.write(data.getBytes());
		}
	}
	
	private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras)
	{
		if(UsbService.SERVICE_CONNECTED == false)
		{
			Intent startService = new Intent(this, service);
			if(extras != null && !extras.isEmpty())
			{
				Set<String> keys = extras.keySet();
				for(String key: keys)
				{
					String extra = extras.getString(key);
					startService.putExtra(key, extra);
				}
			}
			startService(startService);
		}
		Intent bindingIntent = new Intent(this, service);
		bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void setFilters()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
		filter.addAction(UsbService.ACTION_NO_USB);
		filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
		filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
		filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
		registerReceiver(mUsbReceiver, filter);
	}
	
	/*
	 * This handler will be passed to UsbService. Dara received from serial port is displayed through this handler
	 */
	private static class MyHandler extends Handler 
	{
		private final WeakReference<MainActivity> mActivity;

		public MyHandler(MainActivity activity) 
		{
			mActivity = new WeakReference<MainActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
			case UsbService.MESSAGE_FROM_SERIAL_PORT:
				String data = (String) msg.obj;
				mActivity.get().display.append(data);
				break;
			}
		}
	}
	
	/*
	 * Notifications from UsbService will be received here.
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context arg0, Intent arg1) 
		{
			if(arg1.getAction().equals(UsbService.ACTION_USB_PERMISSION_GRANTED)) // USB PERMISSION GRANTED
			{
				Toast.makeText(arg0, "USB Ready", Toast.LENGTH_SHORT).show();
			}else if(arg1.getAction().equals(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)) // USB PERMISSION NOT GRANTED
			{
				Toast.makeText(arg0, "USB Permission not granted", Toast.LENGTH_SHORT).show();
			}else if(arg1.getAction().equals(UsbService.ACTION_NO_USB)) // NO USB CONNECTED
			{
				Toast.makeText(arg0, "No USB connected", Toast.LENGTH_SHORT).show();
			}else if(arg1.getAction().equals(UsbService.ACTION_USB_DISCONNECTED)) // USB DISCONNECTED
			{
				Toast.makeText(arg0, "USB disconnected", Toast.LENGTH_SHORT).show();
			}else if(arg1.getAction().equals(UsbService.ACTION_USB_NOT_SUPPORTED)) // USB NOT SUPPORTED
			{
				Toast.makeText(arg0, "USB device not supported", Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	private final ServiceConnection usbConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) 
		{
			usbService = ((UsbService.UsbBinder) arg1).getService();
			usbService.setHandler(mHandler);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) 
		{
			usbService = null;
		}
	};
}