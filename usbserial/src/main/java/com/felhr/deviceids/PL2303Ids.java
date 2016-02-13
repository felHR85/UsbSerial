package com.felhr.deviceids;

public class PL2303Ids
{
    private PL2303Ids()
    {

    }

    private static final ConcreteDevice[] pl2303Devices = new ConcreteDevice[]
            {
                    new ConcreteDevice (0x04a5, 0x4027),
                    new ConcreteDevice (0x067b, 0x2303),
                    new ConcreteDevice (0x067b, 0x04bb),
                    new ConcreteDevice (0x067b, 0x1234),
                    new ConcreteDevice (0x067b, 0xaaa0),
                    new ConcreteDevice (0x067b, 0xaaa2),
                    new ConcreteDevice (0x067b, 0x0611),
                    new ConcreteDevice (0x067b, 0x0612),
                    new ConcreteDevice (0x067b, 0x0609),
                    new ConcreteDevice (0x067b, 0x331a),
                    new ConcreteDevice (0x067b, 0x0307),
                    new ConcreteDevice (0x067b, 0x0463),
                    new ConcreteDevice (0x0557, 0x2008),
                    new ConcreteDevice (0x0547, 0x2008),
                    new ConcreteDevice (0x04bb, 0x0a03),
                    new ConcreteDevice (0x04bb, 0x0a0e),
                    new ConcreteDevice (0x056e, 0x5003),
                    new ConcreteDevice (0x056e, 0x5004),
                    new ConcreteDevice (0x0eba, 0x1080),
                    new ConcreteDevice (0x0eba, 0x2080),
                    new ConcreteDevice (0x0df7, 0x0620),
                    new ConcreteDevice (0x0584, 0xb000),
                    new ConcreteDevice (0x2478, 0x2008),
                    new ConcreteDevice (0x1453, 0x4026),
                    new ConcreteDevice (0x0731, 0x0528),
                    new ConcreteDevice (0x6189, 0x2068),
                    new ConcreteDevice (0x11f7, 0x02df),
                    new ConcreteDevice (0x04e8, 0x8001),
                    new ConcreteDevice (0x11f5, 0x0001),
                    new ConcreteDevice (0x11f5, 0x0003),
                    new ConcreteDevice (0x11f5, 0x0004),
                    new ConcreteDevice (0x11f5, 0x0005),
                    new ConcreteDevice (0x0745, 0x0001),
                    new ConcreteDevice (0x078b, 0x1234),
                    new ConcreteDevice (0x10b5, 0xac70),
                    new ConcreteDevice (0x079b, 0x0027),
                    new ConcreteDevice (0x0413, 0x2101),
                    new ConcreteDevice (0x0e55, 0x110b),
                    new ConcreteDevice (0x0731, 0x2003),
                    new ConcreteDevice (0x050d, 0x0257),
                    new ConcreteDevice (0x058f, 0x9720),
                    new ConcreteDevice (0x11f6, 0x2001),
                    new ConcreteDevice (0x07aa, 0x002a),
                    new ConcreteDevice (0x05ad, 0x0fba),
                    new ConcreteDevice (0x5372, 0x2303),
                    new ConcreteDevice (0x03f0, 0x0b39),
                    new ConcreteDevice (0x03f0, 0x3139),
                    new ConcreteDevice (0x03f0, 0x3239),
                    new ConcreteDevice (0x03f0, 0x3524),
                    new ConcreteDevice (0x04b8, 0x0521),
                    new ConcreteDevice (0x04b8, 0x0522),
                    new ConcreteDevice (0x054c, 0x0437),
                    new ConcreteDevice (0x11ad, 0x0001),
                    new ConcreteDevice (0x0b63, 0x6530),
                    new ConcreteDevice (0x0b8c, 0x2303),
                    new ConcreteDevice (0x110a, 0x1150)
            };

    public static boolean isDeviceSupported(int vendorId, int productId)
    {
        for(int i=0;i<=pl2303Devices.length-1;i++)
        {
            if(pl2303Devices[i].vendorId == vendorId && pl2303Devices[i].productId == productId )
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
