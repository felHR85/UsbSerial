package com.felhr.tests.utils;

import com.felhr.utils.ProtocolBuffer;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;


public class ProtocolBufferTest extends TestCase {

    private final String onePacket = "$GPAAM,A,A,0.10,N,WPTNME*32\r\n";
    private final String twoPackets = "$GPAAM,A,A,0.10,N,WPTNME*32\r\n$GPGGA,092750.000,5321.6802,N,00630.3372,W,1,8,1.03,61.7,M,55.2,M,,*76\r\n";
    private final String splitPacket = "$GPAAM,A,A,0.10,N,WPTN";
    private final String oneHalfPacket = "$GPAAM,A,A,0.10,N,WPTNME*32\r\n$GPGGA,092750.000";

    private final String[] verySplit ={"$GPAAM,",
        "A",
        ",",
        "A,",
        "0",
        ".",
        "10,N",
        ",WPTNME*32\r\n"};

    private final byte[] rawPacket = new byte[]{0x21, 0x3b, 0x20, 0x40};
    private final byte[] twoRawPackets = new byte[]{0x21, 0x3b, 0x20, 0x40, 0x4a, 0x20, 0x40};
    private final byte[] splitRawPacket = new byte[]{0x21, 0x3b, 0x20, 0x40, 0x4a};

    private final byte[][] verySplitRawPacket = {
            new byte[]{0x21},
            new byte[]{0x3b},
            new byte[]{0x20},
            new byte[]{0x40}};

    private ProtocolBuffer protocolBuffer;
    private final String modeText = ProtocolBuffer.TEXT;
    private final String modeBinary = ProtocolBuffer.BINARY;

    @Test
    public void testOnePacket(){
        protocolBuffer = new ProtocolBuffer(modeText);
        protocolBuffer.setDelimiter("\r\n");
        protocolBuffer.appendData(onePacket.getBytes());

        boolean hasMoreData = protocolBuffer.hasMoreCommands();
        assertTrue(hasMoreData);
        String nextCommand = protocolBuffer.nextTextCommand();
        assertEquals(onePacket, nextCommand);
    }

    @Test
    public void testTwoPackets(){
        protocolBuffer = new ProtocolBuffer(modeText);
        protocolBuffer.setDelimiter("\r\n");
        protocolBuffer.appendData(twoPackets.getBytes());

        StringBuilder builder = new StringBuilder();

        while(protocolBuffer.hasMoreCommands()){
            builder.append(protocolBuffer.nextTextCommand());
        }
        assertEquals(twoPackets, builder.toString());
    }

    @Test
    public void testSplitPackets(){
        protocolBuffer = new ProtocolBuffer(modeText);
        protocolBuffer.setDelimiter("\r\n");
        protocolBuffer.appendData(splitPacket.getBytes());

        boolean hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);
    }

    @Test
    public void testOneHalfPacket(){
        protocolBuffer = new ProtocolBuffer(modeText);
        protocolBuffer.setDelimiter("\r\n");
        protocolBuffer.appendData(oneHalfPacket.getBytes());

        boolean hasMoreData = protocolBuffer.hasMoreCommands();
        assertTrue(hasMoreData);
        String nextCommand = protocolBuffer.nextTextCommand();
        assertEquals("$GPAAM,A,A,0.10,N,WPTNME*32\r\n", nextCommand);

        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        nextCommand = protocolBuffer.nextTextCommand();
        assertNull(nextCommand);
    }

    @Test
    public void testVerySplit(){
        protocolBuffer = new ProtocolBuffer(modeText);
        protocolBuffer.setDelimiter("\r\n");

        protocolBuffer.appendData(verySplit[0].getBytes());
        boolean hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplit[1].getBytes());
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplit[2].getBytes());
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplit[3].getBytes());
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplit[4].getBytes());
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplit[5].getBytes());
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplit[6].getBytes());
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplit[7].getBytes());
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertTrue(hasMoreData);

        String command = protocolBuffer.nextTextCommand();
        assertEquals("$GPAAM,A,A,0.10,N,WPTNME*32\r\n", command);
    }

    @Test
    public void testRawPacket(){
        protocolBuffer = new ProtocolBuffer(modeBinary);
        protocolBuffer.setDelimiter(new byte[]{0x20, 0x40});
        protocolBuffer.appendData(rawPacket);

        boolean hasMoreData = protocolBuffer.hasMoreCommands();
        assertTrue(hasMoreData);

        byte[] command  = protocolBuffer.nextBinaryCommand();
        Assert.assertArrayEquals(command, rawPacket);

        command = protocolBuffer.nextBinaryCommand();
        assertNull(command);
    }

    @Test
    public void testTwoRawPackets(){
        protocolBuffer = new ProtocolBuffer(modeBinary);
        protocolBuffer.setDelimiter(new byte[]{0x20, 0x40});
        protocolBuffer.appendData(twoRawPackets);

        boolean hasMoreData = protocolBuffer.hasMoreCommands();
        assertTrue(hasMoreData);

        byte[] command1 = protocolBuffer.nextBinaryCommand();
        Assert.assertArrayEquals(command1, new byte[]{0x21, 0x3b, 0x20, 0x40});

        hasMoreData = protocolBuffer.hasMoreCommands();
        assertTrue(hasMoreData);

        byte[] command2 = protocolBuffer.nextBinaryCommand();
        Assert.assertArrayEquals(command2, new byte[]{0x4a, 0x20, 0x40});

        command2 = protocolBuffer.nextBinaryCommand();
        assertNull(command2);
    }

    @Test
    public void testSplitRawPacket(){
        protocolBuffer = new ProtocolBuffer(modeBinary);
        protocolBuffer.setDelimiter(new byte[]{0x20, 0x40});
        protocolBuffer.appendData(splitRawPacket);

        boolean hasMoreData = protocolBuffer.hasMoreCommands();
        assertTrue(hasMoreData);

        byte[] command1 = protocolBuffer.nextBinaryCommand();
        Assert.assertArrayEquals(command1, new byte[]{0x21, 0x3b, 0x20, 0x40});

        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);
    }

    @Test
    public void testVerySplitRawPacket(){
        protocolBuffer = new ProtocolBuffer(modeBinary);
        protocolBuffer.setDelimiter(new byte[]{0x20, 0x40});

        protocolBuffer.appendData(verySplitRawPacket[0]);
        boolean hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplitRawPacket[1]);
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplitRawPacket[2]);
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertFalse(hasMoreData);

        protocolBuffer.appendData(verySplitRawPacket[3]);
        hasMoreData = protocolBuffer.hasMoreCommands();
        assertTrue(hasMoreData);

        byte[] command = protocolBuffer.nextBinaryCommand();
        Assert.assertArrayEquals(command, new byte[]{0x21, 0x3b, 0x20, 0x40});
    }
}
