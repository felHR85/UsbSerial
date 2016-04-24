package com.felhr.usbserial;


public interface UsbSpiInterface
{
    // Clock dividers;
    int DIVIDER_2 = 2;
    int DIVIDER_4 = 4;
    int DIVIDER_8 = 8;
    int DIVIDER_16 = 16;
    int DIVIDER_32 = 32;
    int DIVIDER_64 = 64;
    int DIVIDER_128 = 128;

    // Common SPI operations
    boolean connectSPI();
    void writeMOSI(byte[] buffer);
    void readMISO(int lengthBuffer);
    void setClock(int clockDivider);
    void selectSlave(int nSlave);
    void setMISOCallback(UsbMISOCallback misoCallback);

    // Status information
    int getClockDivider();
    int getSelectedSlave();

    interface UsbMISOCallback
    {
        int onReceivedData(byte[] data);
    }
}
