UsbSerial
=========

Usb serial controller for Android.

Devices Supported
--------------------------------------
[CP210X devices](http://www.silabs.com/products/mcu/pages/usbtouartbridgevcpdrivers.aspx) Default: 9600,8,1,None,flow off

[Bluegiga BLED112 Dongle](https://www.bluegiga.com/en-US/products/bluetooth-4.0-modules/bled112-bluetooth-smart-dongle/) Default 115200,8,1,None,flow off

How to use it?
--------------------------------------
Instantiate a new object of the desired device you want to handle
~~~
UsbDevice device;
UsbDeviceConnection usbConnection;
...
CP2102SerialDevice cp2102 = new CP2102SerialDevice(device, usbConnection);
//UsbSerialDevice bled112 = new BLED112SerialDevice(device, usbConnection);
~~~

Open the device and set it up as desired
~~~~
cp2102.open();
cp2102.setBaudRate(115200);
cp2102.setDataBits(UsbSerialInterface.DATA_BITS_8);
cp2102.setParity(UsbSerialInterface.PARITY_ODD);
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
cp2102.read(mCallback);
~~~

Write something
~~~
cp2102.write("DATA".getBytes()); // Important: This operation is synchronous
~~~

Close the device:
~~~
cp2102.close();
~~~

In Android usb api, when a usb device has been close it must be reopened
~~~
UsbDevice device;
...
UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
manager.openDevice(UsbDevice device)
~~~


TO-DO
--------------------------------------
- Add more devices (pretty obvious)
- RTS/CTS and DSR/DTR functions needed to raise hardware flow control signals
- Eventually, write(byte[] buffer) function will be asynchronous
- A serious refactoring is needed to move common logic to UsbSerialDevice (This would help other people to add new devices)
- Whatever you think can be a good addition




