package com.felhr.usbserial;


public interface UsbGPIOInterface {
    int LOW = 0;
    int HIGH = 1;

    int OUT = 0;
    int IN = 1;

    void setPinValue(int pin, int value);
    void setPinMode(int pin, int mode);
    void setPinMode(int pin, int mode, int defaultValue);

    int readPin(int pin); // Synchronous GPIO Read

    interface UsbGPIOCallback {
        void onGPIOPinChanged(int pin, int value);
    }
}
