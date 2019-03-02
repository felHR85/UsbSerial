import serial
import sys
import os

port = sys.argv[1]
speed = sys.argv[2]

test_sizes = [1024, 2048, 16384, 65535, 131072]

for i in range(0,4):
    comm = serial.Serial(port, int(speed))
    print("Creating " + str(test_sizes[i] + " bytes buffer"))
    data_tx = os.urandom(test_sizes[i])
    print("Sending buffer of " + str(test_sizes[i]) + " bytes")
    comm.write(data_tx)
    print("Receiving " + str(test_sizes[i]) + " bytes buffer")
    data_rx = comm.read(test_sizes[i])
    
    if data_tx == data_rx:
        print("Success: " + str(test_sizes[i]) + " bytes buffer was transmitted correctly")
    else:
        print("Error: " + str(test_sizes[i]) + " bytes buffer was not transmitted correctly")
