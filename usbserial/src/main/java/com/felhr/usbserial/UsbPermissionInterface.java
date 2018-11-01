package com.felhr.usbserial;

import java.util.List;


public interface UsbPermissionInterface {
    void permissionsCallback(List<UsbSerialDevice> serialPorts);
}
