UsbSerial
=========

Usb serial controller for Android. For more information, there is [a more complete description](http://felhr85.net/2014/11/11/usbserial-a-serial-port-driver-library-for-android-v2-0/) and [an example app](https://github.com/felHR85/SerialPortExample).

Devices Supported
--------------------------------------
[CP210X devices](http://www.silabs.com/products/mcu/pages/usbtouartbridgevcpdrivers.aspx) Default: 9600,8,1,None,flow off

[CDC devices](https://en.wikipedia.org/wiki/USB_communications_device_class) Default 115200,8,1,None,flow off

[FTDI devices](http://www.ftdichip.com/FTProducts.htm) Default: 9600,8,1,None,flow off

[PL2303 devices](http://www.prolific.com.tw/US/ShowProduct.aspx?p_id=225&pcid=41) Default 9600,8,1,None,flow off

[CH34x devices](https://www.olimex.com/Products/Breadboarding/BB-CH340T/resources/CH340DS1.PDF) Default 9600,8,1,None,flow off

How to use it?
--------------------------------------
Instantiate a new object of the UsbSerialDevice class
~~~
UsbDevice device;
UsbDeviceConnection usbConnection;
...
UsbSerialDevice serial = UsbSerialDevice.createUsbSerialDevice(device, usbConnection); 
~~~

Open the device and set it up as desired
~~~~
serial.open();
serial.setBaudRate(115200);
serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
serial.setParity(UsbSerialInterface.PARITY_ODD);
~~~~

There is no need to be polling if you want to perform a bulk transaction to a IN endpoint. Define a simply callback
~~~
private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {

		@Override
		public void onReceivedData(byte[] arg0) 
		{
			// Code here :)
		}
		
};
~~~

And pass a reference of it
~~~
serial.read(mCallback);
~~~

Write something
~~~
serial.write("DATA".getBytes()); // Async-like operation now! :)
~~~

Close the device:
~~~
serial.close();
~~~

In Android usb api, when a usb device has been close it must be reopened
~~~
UsbDevice device;
...
UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
manager.openDevice(UsbDevice device)
~~~


Gradle
--------------------------------------
Add the jitpack repo to your your project's build.gradle at the end of repositories

/build.gradle
```groovy
allprojects {
	repositories {
		jcenter()
		maven { url "https://jitpack.io" }
	}
}
```

Then add the dependency to your module's build.gradle:

/app/build.gradle
```groovy
compile 'com.github.felHR85:UsbSerial:v3.2'
```

TO-DO
--------------------------------------
- RTS/CTS and DSR/DTR functions needed to raise hardware flow control signals






