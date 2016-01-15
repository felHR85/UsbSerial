package com.felhr.deviceids;

public class CP210xIds
{
    /* Different products and vendors of CP210x family
    // From current cp210x linux driver:
    https://github.com/torvalds/linux/blob/164c09978cebebd8b5fc198e9243777dbaecdfa0/drivers/usb/serial/cp210x.c
    */
    private static final ConcreteDevice[] cp210xDevices = new ConcreteDevice[]
            {
                    new ConcreteDevice(0x045B, 0x0053),
                    new ConcreteDevice(0x0471, 0x066A),
                    new ConcreteDevice(0x0489, 0xE000),
                    new ConcreteDevice(0x0489, 0xE003),
                    new ConcreteDevice(0x0745, 0x1000),
                    new ConcreteDevice(0x0846, 0x1100),
                    new ConcreteDevice(0x08e6, 0x5501),
                    new ConcreteDevice(0x08FD, 0x000A),
                    new ConcreteDevice(0x0BED, 0x1100),
                    new ConcreteDevice(0x0BED, 0x1101),
                    new ConcreteDevice(0x0FCF, 0x1003),
                    new ConcreteDevice(0x0FCF, 0x1004),
                    new ConcreteDevice(0x0FCF, 0x1006),
                    new ConcreteDevice(0x0FDE, 0xCA05),
                    new ConcreteDevice(0x10A6, 0xAA26),
                    new ConcreteDevice(0x10AB, 0x10C5),
                    new ConcreteDevice(0x10B5, 0xAC70),
                    new ConcreteDevice(0x10C4, 0x0F91),
                    new ConcreteDevice(0x10C4, 0x1101),
                    new ConcreteDevice(0x10C4, 0x1601),
                    new ConcreteDevice(0x10C4, 0x800A),
                    new ConcreteDevice(0x10C4, 0x803B),
                    new ConcreteDevice(0x10C4, 0x8044),
                    new ConcreteDevice(0x10C4, 0x804E),
                    new ConcreteDevice(0x10C4, 0x8053),
                    new ConcreteDevice(0x10C4, 0x8054),
                    new ConcreteDevice(0x10C4, 0x8066),
                    new ConcreteDevice(0x10C4, 0x806F),
                    new ConcreteDevice(0x10C4, 0x807A),
                    new ConcreteDevice(0x10C4, 0x80C4),
                    new ConcreteDevice(0x10C4, 0x80CA),
                    new ConcreteDevice(0x10C4, 0x80DD),
                    new ConcreteDevice(0x10C4, 0x80F6),
                    new ConcreteDevice(0x10C4, 0x8115),
                    new ConcreteDevice(0x10C4, 0x813D),
                    new ConcreteDevice(0x10C4, 0x813F),
                    new ConcreteDevice(0x10C4, 0x814A),
                    new ConcreteDevice(0x10C4, 0x814B),
                    new ConcreteDevice(0x2405, 0x0003),
                    new ConcreteDevice(0x10C4, 0x8156),
                    new ConcreteDevice(0x10C4, 0x815E),
                    new ConcreteDevice(0x10C4, 0x815F),
                    new ConcreteDevice(0x10C4, 0x818B),
                    new ConcreteDevice(0x10C4, 0x819F),
                    new ConcreteDevice(0x10C4, 0x81A6),
                    new ConcreteDevice(0x10C4, 0x81A9),
                    new ConcreteDevice(0x10C4, 0x81AC),
                    new ConcreteDevice(0x10C4, 0x81AD),
                    new ConcreteDevice(0x10C4, 0x81C8),
                    new ConcreteDevice(0x10C4, 0x81E2),
                    new ConcreteDevice(0x10C4, 0x81E7),
                    new ConcreteDevice(0x10C4, 0x81E8),
                    new ConcreteDevice(0x10C4, 0x81F2),
                    new ConcreteDevice(0x10C4, 0x8218),
                    new ConcreteDevice(0x10C4, 0x822B),
                    new ConcreteDevice(0x10C4, 0x826B),
                    new ConcreteDevice(0x10C4, 0x8281),
                    new ConcreteDevice(0x10C4, 0x8293),
                    new ConcreteDevice(0x10C4, 0x82F9),
                    new ConcreteDevice(0x10C4, 0x8341),
                    new ConcreteDevice(0x10C4, 0x8382),
                    new ConcreteDevice(0x10C4, 0x83A8),
                    new ConcreteDevice(0x10C4, 0x83D8),
                    new ConcreteDevice(0x10C4, 0x8411),
                    new ConcreteDevice(0x10C4, 0x8418),
                    new ConcreteDevice(0x10C4, 0x846E),
                    new ConcreteDevice(0x10C4, 0x8477),
                    new ConcreteDevice(0x10C4, 0x85EA),
                    new ConcreteDevice(0x10C4, 0x85EB),
                    new ConcreteDevice(0x10C4, 0x85F8),
                    new ConcreteDevice(0x10C4, 0x8664),
                    new ConcreteDevice(0x10C4, 0x8665),
                    new ConcreteDevice(0x10C4, 0x88A4),
                    new ConcreteDevice(0x10C4, 0x88A5),
                    new ConcreteDevice(0x10C4, 0xEA60),
                    new ConcreteDevice(0x10C4, 0xEA61),
                    new ConcreteDevice(0x10C4, 0xEA70),
                    new ConcreteDevice(0x10C4, 0xEA80),
                    new ConcreteDevice(0x10C4, 0xEA71),
                    new ConcreteDevice(0x10C4, 0xF001),
                    new ConcreteDevice(0x10C4, 0xF002),
                    new ConcreteDevice(0x10C4, 0xF003),
                    new ConcreteDevice(0x10C4, 0xF004),
                    new ConcreteDevice(0x10C5, 0xEA61),
                    new ConcreteDevice(0x10CE, 0xEA6A),
                    new ConcreteDevice(0x13AD, 0x9999),
                    new ConcreteDevice(0x1555, 0x0004),
                    new ConcreteDevice(0x166A, 0x0201),
                    new ConcreteDevice(0x166A, 0x0301),
                    new ConcreteDevice(0x166A, 0x0303),
                    new ConcreteDevice(0x166A, 0x0304),
                    new ConcreteDevice(0x166A, 0x0305),
                    new ConcreteDevice(0x166A, 0x0401),
                    new ConcreteDevice(0x166A, 0x0101),
                    new ConcreteDevice(0x16D6, 0x0001),
                    new ConcreteDevice(0x16DC, 0x0010),
                    new ConcreteDevice(0x16DC, 0x0011),
                    new ConcreteDevice(0x16DC, 0x0012),
                    new ConcreteDevice(0x16DC, 0x0015),
                    new ConcreteDevice(0x17A8, 0x0001),
                    new ConcreteDevice(0x17A8, 0x0005),
                    new ConcreteDevice(0x17F4, 0xAAAA),
                    new ConcreteDevice(0x1843, 0x0200),
                    new ConcreteDevice(0x18EF, 0xE00F),
                    new ConcreteDevice(0x1ADB, 0x0001),
                    new ConcreteDevice(0x1BE3, 0x07A6),
                    new ConcreteDevice(0x1E29, 0x0102),
                    new ConcreteDevice(0x1E29, 0x0501),
                    new ConcreteDevice(0x1FB9, 0x0100),
                    new ConcreteDevice(0x1FB9, 0x0200),
                    new ConcreteDevice(0x1FB9, 0x0201),
                    new ConcreteDevice(0x1FB9, 0x0202),
                    new ConcreteDevice(0x1FB9, 0x0203),
                    new ConcreteDevice(0x1FB9, 0x0300),
                    new ConcreteDevice(0x1FB9, 0x0301),
                    new ConcreteDevice(0x1FB9, 0x0302),
                    new ConcreteDevice(0x1FB9, 0x0303),
                    new ConcreteDevice(0x1FB9, 0x0400),
                    new ConcreteDevice(0x1FB9, 0x0401),
                    new ConcreteDevice(0x1FB9, 0x0402),
                    new ConcreteDevice(0x1FB9, 0x0403),
                    new ConcreteDevice(0x1FB9, 0x0404),
                    new ConcreteDevice(0x1FB9, 0x0600),
                    new ConcreteDevice(0x1FB9, 0x0601),
                    new ConcreteDevice(0x1FB9, 0x0602),
                    new ConcreteDevice(0x1FB9, 0x0700),
                    new ConcreteDevice(0x1FB9, 0x0701),
                    new ConcreteDevice(0x3195, 0xF190),
                    new ConcreteDevice(0x3195, 0xF280),
                    new ConcreteDevice(0x3195, 0xF281),
                    new ConcreteDevice(0x413C, 0x9500)
            };

    public static boolean isDeviceSupported(int vendorId, int productId)
    {
        for(int i=0;i<=cp210xDevices.length-1;i++)
        {
            if(cp210xDevices[i].vendorId == vendorId && cp210xDevices[i].productId == productId )
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
