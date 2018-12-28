package com.felhr.tests.utils;

import com.felhr.utils.ProtocolBuffer;

import junit.framework.TestCase;

import org.junit.Test;


public class ProtocolBufferTest extends TestCase {

    private final String NMEA1 = "$GPAAM,A,A,0.10,N,WPTNME*32\r\n";
    private final String NMEA2 = "$GPAAM,A,A,0.10,N,WPTNME*32\r\n$GPGGA,092750.000,5321.6802,N,00630.3372,W,1,8,1.03,61.7,M,55.2,M,,*76\r\n";
    private final String NMEA3 = "$GPAAM,A,A,0.10,N,WPTN";
    private final String NMEA4 = "$GPAAM,A,A,0.10,N,WPTNME*32\r\n$GPGGA,092750.000";

    private ProtocolBuffer protocolBuffer;
    private final String modeText = ProtocolBuffer.TEXT;
    private final String modeBinary = ProtocolBuffer.BINARY;

    @Test
    public void testNMEA1(){
        protocolBuffer = new ProtocolBuffer(modeText);
        protocolBuffer.setTrailChars("\r\n");
        protocolBuffer.appendData(NMEA1.getBytes());

        boolean hasMoreData = protocolBuffer.hasMoreCommands();
        assertTrue(hasMoreData);
        String nextCommand = protocolBuffer.nextCommand();
        assertEquals(NMEA1, nextCommand);
    }
}
