# UsbSerial test: Validate single packet
# args:
#   port (eg /dev/ttyUSB0)
#   size in bytes (eg 1024)
#   speed in bauds (eg 115200)

import serial
import sys
import os

port = sys.argv[1]
size = sys.argv[2]
speed = sys.argv[3]

comm = serial.Serial(port, int(speed))

data_tx = os.urandom(int(size))

comm.write(data_tx)

data_rx = comm.read(int(size))

if data_tx == data_rx:
    print("Success: Data was transmitted correctly")
else:
    print("Error: Data was not transmitted")