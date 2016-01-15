package com.felhr.deviceids;

public class XdcVcpIds
{
	/*
	 * Werner Wolfrum (w.wolfrum@wolfrum-elektronik.de)
	 */

    /* Different products and vendors of XdcVcp family
    */
    private static final ConcreteDevice[] xdcvcpDevices = new ConcreteDevice[]
            {
                    new ConcreteDevice(0x264D, 0x0232), // VCP (Virtual Com Port)
                    new ConcreteDevice(0x264D, 0x0120)  // USI (Universal Sensor Interface)
            };

    public static boolean isDeviceSupported(int vendorId, int productId)
    {
        for(int i=0;i<=xdcvcpDevices.length-1;i++)
        {
            if(xdcvcpDevices[i].vendorId == vendorId && xdcvcpDevices[i].productId == productId )
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
