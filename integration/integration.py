# UsbSerial test: Integration test
# args:
#   port (eg /dev/ttyUSB0)
#   speed in bauds (eg 115200)

import serial
import sys
import os

class style():
    RED = lambda x: '\033[31m' + str(x)
    GREEN = lambda x: '\033[32m' + str(x)
    BLUE = lambda x: '\033[34m' + str(x)
    RESET = lambda x: '\033[0m' + str(x)

port = sys.argv[1]
speed = sys.argv[2]

test_sizes = [1024, 2048, 16384, 65535, 131072]

for i in range(0,5):
    comm = serial.Serial(port, int(speed))
    print("Creating " + str(test_sizes[i]) + " bytes buffer")
    data_tx = os.urandom(test_sizes[i])
    print("Sending buffer of " + str(test_sizes[i]) + " bytes")
    comm.write(data_tx)
    print("Receiving " + str(test_sizes[i]) + " bytes buffer")
    data_rx = comm.read(test_sizes[i])
    
    if data_tx == data_rx:
        print(style.GREEN("Success: " + str(test_sizes[i]) + " bytes buffer was transmitted correctly"))
    else:
        print(style.RED("Error: " + str(test_sizes[i]) + " bytes buffer was not transmitted correctly"))
    print(style.RESET(""))
