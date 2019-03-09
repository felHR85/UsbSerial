CHANGELOG
=========

Release 6.0.4
--------------------------------------
- Proguard rules
- FTDI driver improved again

Release 6.0.3
--------------------------------------
- VID/PID pairs are sorted and searched in a faster way.
- FTDI driver improved.

Release 6.0.2
--------------------------------------
- Solved issue when disconnecting multiple serial ports.

Release 6.0.1
--------------------------------------
- Internal serial buffer now uses [Okio](https://github.com/square/okio). This erases the 16kb write
limitation from previous versions and reduces memory footprint.
- Improved CP2102 driver and added more VID/PID pairs.
- Added a [utility class for handling the common problem of split received information in some chipsets](https://github.com/felHR85/UsbSerial/blob/master/usbserial/src/main/java/com/felhr/utils/ProtocolBuffer.java).

