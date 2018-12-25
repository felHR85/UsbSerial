package com.felhr.deviceids;

public class CP2130Ids
{
    private static final ConcreteDevice[] cp2130Devices = new ConcreteDevice[]{
            new ConcreteDevice(0x10C4, 0x87a0),
    };

    public static boolean isDeviceSupported(int vendorId, int productId)
    {
        for(int i=0;i<=cp2130Devices.length-1;i++)
        {
            if(cp2130Devices[i].vendorId == vendorId && cp2130Devices[i].productId == productId )
            {
                return true;
            }
        }
        return false;
    }

    private static class ConcreteDevice
    {
        public int vendorId;
        public int productId;

        public ConcreteDevice(int vendorId, int productId)
        {
            this.vendorId = vendorId;
            this.productId = productId;
        }
    }
}
