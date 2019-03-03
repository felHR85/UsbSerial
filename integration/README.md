UsbSerial Integration Tests
===========================
For the purpose of helping people contributing to UsbSerial a little set of integration tests have been added. It consists in two parts.
- Python script integration.py that sends a series packets (1kb, 2kb, 8kb, 64kb and 128kb) and validates that those packets are received back correctly.
- Integration Android app that implements UsbSerial and just receives and sends back the packets sent by the python script.

Requirements
--------------------------------------
- Windows/OSX/Linux with Python 3 installed
- [PySerial](https://pypi.org/project/pyserial/)
- Android phone with Android 3.1 and with USB OTG capabilities

Steps
--------------------------------------
Let's say we want to test UsbSerial transmitting at 115200 bauds and our PC port is /dev/ttyUSB0
- [Modify UsbService in Integration app to 115200 bauds](https://github.com/felHR85/UsbSerial/blob/integration_tests/integrationapp/src/main/java/com/felhr/integrationapp/UsbService.java#L61).
- Compile and install Integration app on your device.
- Connect your phone to a serial device at one end and your PC at the other end.
- Run python integration.py /dev/ttyUSB0 115200

Other Scripts
--------------------------------------
- send_packet.py  (python send_packet.py /dev/ttyUSB0 1024 115200)
- validate_serial_tx.py (python validate_serial_tx.py /dev/ttyUSB0 1024 115200)