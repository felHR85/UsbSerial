#!bin/bash

# UsbSerial
#
# This test generates a number of files specified by the user and send them through the serial port.
#
# args:
#      -p: serial port (ttyUSB0, ttyUSB1..)
#      -b: baud rate
#      -t: number of times this test will be repeated
#      -s: size of the random files generated for testing purposes

while getopts p:t:s:b: OPTION;
do
case $OPTION 
	in
	p)      PORT=$OPTARG;;
    t)	    TIMES=$OPTARG;;
	s) 	    SIZE=$OPTARG;;
	b)      BAUD=$OPTARG;;
esac
done

stty -F $PORT $BAUD

for i in $(seq 1 $TIMES);
do	
    dd if=/dev/urandom of=$i bs=$SIZE count=1 status=none
	echo "Packet $i of $SIZE was created"
	cat $i > $PORT
	echo "Packet $i of $SIZE was sent"
	
done

rm [0-9]*
