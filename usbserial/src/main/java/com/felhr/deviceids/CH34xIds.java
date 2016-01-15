package com.felhr.deviceids;

public class CH34xIds
{
    private CH34xIds()
    {

    }

    private static final ConcreteDevice[] ch34xDevices = new ConcreteDevice[]
            {
                    new ConcreteDevice(0x4348, 0x5523),
                    new ConcreteDevice(0x1a86, 0x7523),
                    new ConcreteDevice(0x1a86, 0x5523),
                    new ConcreteDevice(0x1a86, 0x0445)
            };

    public static boolean isDeviceSupported(int vendorId, int productId)
    {
        for(int i=0;i<=ch34xDevices.length-1;i++)
        {
            if(ch34xDevices[i].vendorId == vendorId &&ch34xDevices[i].productId == productId )
                return true;
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
