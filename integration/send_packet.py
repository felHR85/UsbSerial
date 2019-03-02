import serial
import sys
import os

port = sys.argv[1]
size = sys.argv[2]
speed = sys.argv[3]

comm = serial.Serial(port, int(speed))

data_tx = os.urandom(int(size))

bytes_sent = comm.write(data_tx)

print(str(bytes_sent))