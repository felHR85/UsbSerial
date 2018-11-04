package com.felhr.usbserial;


public interface UsbGPIOInterface {
    int LOW = 0;
    int HIGH = 1;

    void setToLow(int pin);
    void setToHigh(int pin);

    int readPin(int pin); // Synchronous GPIO Read

    interface UsbGPIOCallback {
        void onGPIOPinChanged(int pin, int value);
    }
}
