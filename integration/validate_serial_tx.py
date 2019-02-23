import serial
import sys
import os

port = sys.argv[0]
size = sys.argv[1]
speed = sys.argv[2]

comm = serial.Serial(port, int(speed))

data_tx = os.urandom(size)

comm.write(data_tx)

data_rx = comm.read(size)

if data_tx == data_rx:
    print("Success: Data was transmitted correctly")
else:
    print("Error: Data was not transmitted")




