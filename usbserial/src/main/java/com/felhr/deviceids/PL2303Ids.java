package com.felhr.deviceids;

import static com.felhr.deviceids.Helpers.createTable;
import static com.felhr.deviceids.Helpers.createDevice;

public class PL2303Ids
{
    private PL2303Ids()
    {

    }

    private static final long[] pl2303Devices = createTable(
            createDevice(0x04a5, 0x4027),
            createDevice(0x067b, 0x2303),
            createDevice(0x067b, 0x04bb),
            createDevice(0x067b, 0x1234),
            createDevice(0x067b, 0xaaa0),
            createDevice(0x067b, 0xaaa2),
            createDevice(0x067b, 0x0611),
            createDevice(0x067b, 0x0612),
            createDevice(0x067b, 0x0609),
            createDevice(0x067b, 0x331a),
            createDevice(0x067b, 0x0307),
            createDevice(0x067b, 0x0463),
            createDevice(0x0557, 0x2008),
            createDevice(0x0547, 0x2008),
            createDevice(0x04bb, 0x0a03),
            createDevice(0x04bb, 0x0a0e),
            createDevice(0x056e, 0x5003),
            createDevice(0x056e, 0x5004),
            createDevice(0x0eba, 0x1080),
            createDevice(0x0eba, 0x2080),
            createDevice(0x0df7, 0x0620),
            createDevice(0x0584, 0xb000),
            createDevice(0x2478, 0x2008),
            createDevice(0x1453, 0x4026),
            createDevice(0x0731, 0x0528),
            createDevice(0x6189, 0x2068),
            createDevice(0x11f7, 0x02df),
            createDevice(0x04e8, 0x8001),
            createDevice(0x11f5, 0x0001),
            createDevice(0x11f5, 0x0003),
            createDevice(0x11f5, 0x0004),
            createDevice(0x11f5, 0x0005),
            createDevice(0x0745, 0x0001),
            createDevice(0x078b, 0x1234),
            createDevice(0x10b5, 0xac70),
            createDevice(0x079b, 0x0027),
            createDevice(0x0413, 0x2101),
            createDevice(0x0e55, 0x110b),
            createDevice(0x0731, 0x2003),
            createDevice(0x050d, 0x0257),
            createDevice(0x058f, 0x9720),
            createDevice(0x11f6, 0x2001),
            createDevice(0x07aa, 0x002a),
            createDevice(0x05ad, 0x0fba),
            createDevice(0x5372, 0x2303),
            createDevice(0x03f0, 0x0b39),
            createDevice(0x03f0, 0x3139),
            createDevice(0x03f0, 0x3239),
            createDevice(0x03f0, 0x3524),
            createDevice(0x04b8, 0x0521),
            createDevice(0x04b8, 0x0522),
            createDevice(0x054c, 0x0437),
            createDevice(0x11ad, 0x0001),
            createDevice(0x0b63, 0x6530),
            createDevice(0x0b8c, 0x2303),
            createDevice(0x110a, 0x1150),
            createDevice(0x0557, 0x2008)
    );

    public static boolean isDeviceSupported(int vendorId, int productId)
    {
        return Helpers.exists(pl2303Devices, vendorId, productId);
    }
}
