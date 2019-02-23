package com.felhr.deviceids;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DeviceIdTest {

    @Test
    public void testLongPacking() {
        TestCase[] cases = {
                new TestCase(0x08FD, 0x000A),
                new TestCase(0x0BED, 0x1100),
                new TestCase(0x0BED, 0x1101),
                new TestCase(0x0FCF, 0x1003),
                new TestCase(0x0FCF, 0x1004),
                new TestCase(0x0FCF, 0x1006),
                new TestCase(0x0FDE, 0xCA05),
                new TestCase(0x10A6, 0xAA26),
                new TestCase(0x10AB, 0x10C5),
                new TestCase(0x10B5, 0xAC70),
                new TestCase(0x10C4, 0x0F91),
                new TestCase(0x10C4, 0x1101),
                new TestCase(0x10C4, 0x1601),
                new TestCase(0x10C4, 0x800A),
                new TestCase(0x10C4, 0x803B),
                new TestCase(0x10C4, 0x8044),
                new TestCase(0x10C4, 0x804E),
                new TestCase(0x10C4, 0x8053),
                new TestCase(0x10C4, 0x8054),
                new TestCase(0x10C4, 0x8066),
                new TestCase(0x10C4, 0x806F),
                new TestCase(0x10C4, 0x807A),
                new TestCase(0x10C4, 0x80C4),
                new TestCase(0x10C4, 0x80CA),
                new TestCase(0x10C4, 0x80DD),
                new TestCase(0x10C4, 0x80F6),
                new TestCase(0x10C4, 0x8115),
                new TestCase(0x10C4, 0x813D),
                new TestCase(0x10C4, 0x813F),
                new TestCase(0x10C4, 0x814A),
                new TestCase(0x10C4, 0x814B),
                new TestCase(0x2405, 0x0003),
                new TestCase(0x10C4, 0x8156)
        };

        for (TestCase tc : cases) {
            Assert.assertTrue(CP210xIds.isDeviceSupported(tc.vendor, tc.product));
            Assert.assertFalse(FTDISioIds.isDeviceSupported(tc.vendor, tc.product));
        }
    }


    class TestCase {
        final int vendor, product;

        TestCase(int vendor, int product) {
            this.vendor = vendor;
            this.product = product;
        }
    }
}
