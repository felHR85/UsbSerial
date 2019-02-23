package com.felhr.deviceids;

import java.util.Arrays;

class Helpers {

    /**
     * Create a device id, since they are 4 bytes each, we can pack the pair in an long.
     */
    static long createDevice(int vendorId, int productId) {
        return ((long) vendorId) << 32 | (productId & 0xFFFF_FFFFL);
    }

    /**
     * Creates a sorted table.
     * This way, we can use binarySearch to find whether the entry exists.
     */
    static long[] createTable(long ... entries) {
        Arrays.sort(entries);
        return entries;
    }

    static boolean exists(long[] devices, int vendorId, int productId) {
        return Arrays.binarySearch(devices, createDevice(vendorId, productId)) >= 0;
    }
}
