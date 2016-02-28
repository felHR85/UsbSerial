UsbSerial
=========

Usb serial controller for Android. For more information, there is [a more complete description](http://felhr85.net/2014/11/11/usbserial-a-serial-port-driver-library-for-android-v2-0/).

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
```java
serial.open();
serial.setBaudRate(115200);
serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
serial.setParity(UsbSerialInterface.PARITY_ODD);
serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF); 
```

If flow control is needed (currently only supported in CP201x and FTDI devices)
```java
/**
Values:
    UsbSerialInterface.FLOW_CONTROL_OFF
    UsbSerialInterface.FLOW_CONTROL_RTS_CTS 
    UsbSerialInterface.FLOW_CONTROL_DSR_DTR
**/
serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_RTS_CTS);
```

There is no need to be polling if you want to perform a bulk transaction to a IN endpoint. Define a simply callback
```java
private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {

		@Override
		public void onReceivedData(byte[] arg0) 
		{
			// Code here :)
		}
		
};
```

And pass a reference of it
```java
serial.read(mCallback);
```

Changes in the CTS and DSR lines will be received in the same manner. Define a callback and pass a reference of it.
```java
private UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback() {
        @Override
        public void onCTSChanged(boolean state) {
           // Code here :)
        }
    };
    
private UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback() {
        @Override
        public void onDSRChanged(boolean state) {
           // Code here :)
        }
    };
    
serial.getCTS(ctsCallback);
//serial.getDSR(dsrCallback);
```



Write something
```java
serial.write("DATA".getBytes()); // Async-like operation now! :)
```

Raise the state of the RTS or DTR lines
```java
serial.setRTS(true); // Raised
serial.setRTS(false); // Not Raised
serial.setDTR(true); // Raised
serial.setDTR(false); // Not Raised
```

Close the device:
```java
serial.close();
```

I recommend using UsbSerial as shown above but if you want to perform write and read operations in synchronous way it is possible using these methods:
```java
public boolean syncOpen();
public int syncWrite(byte[] buffer, int timeout)
public int syncRead(byte[] buffer, int timeout)
public void syncClose();
```


In Android usb api, when a usb device has been close it must be reopened
```java
UsbDevice device;
...
UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
manager.openDevice(UsbDevice device)
```


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
compile 'com.github.felHR85:UsbSerial:4.1.1'
```

TO-DO
--------------------------------------
- RTS/CTS and DSR/DTR implementations for PL2303, CDC and CH430/431






