package com.felhr.tests.usbserial;

import com.felhr.usbserial.SerialBuffer;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;


public class SerialBufferTest extends TestCase {

    private static final int BIG_BUFFER_1 = 64 * 1024;
    private static final int BIG_BUFFER_2 = 1024 * 1024;

    private static final String text1 = "HOLA";
    private static final byte[] bigBuffer = new byte[BIG_BUFFER_1];
    private static final byte[] bigBuffer2 = new byte[BIG_BUFFER_2];

    private SerialBuffer serialBuffer;

    @Test
    public void testSimpleWriteBuffer(){
        serialBuffer = new SerialBuffer(true);
        serialBuffer.putWriteBuffer(text1.getBytes());
        byte[] dataReceived = serialBuffer.getWriteBuffer();
        assertEquals(text1, new String(dataReceived));
    }

    @Test
    public void testBigSimpleWriteBuffer(){
        Arrays.fill(bigBuffer, (byte) 0x0A);
        serialBuffer = new SerialBuffer(true);
        serialBuffer.putWriteBuffer(bigBuffer);
        byte[] dataReceived = serialBuffer.getWriteBuffer();
        Assert.assertArrayEquals(bigBuffer, dataReceived);
    }

    @Test
    public void testSuperBigSimpleWriteBuffer(){
        new Random().nextBytes(bigBuffer2);
        serialBuffer = new SerialBuffer(true);
        serialBuffer.putWriteBuffer(bigBuffer2);
        byte[] dataReceived = serialBuffer.getWriteBuffer();
        Assert.assertArrayEquals(bigBuffer2, dataReceived);
    }

    @Test
    public void testSimpleWriteBufferAsync1(){
        new Random().nextBytes(bigBuffer2);
        serialBuffer = new SerialBuffer(true);

        WriterThread writerThread = new WriterThread(serialBuffer, bigBuffer2, false);
        writerThread.start();

        byte[] dataReceived = serialBuffer.getWriteBuffer();
        Assert.assertArrayEquals(bigBuffer2, dataReceived);
    }

    @Test
    public void testSimpleWriteBufferAsync2(){
        new Random().nextBytes(bigBuffer2);
        serialBuffer = new SerialBuffer(true);

        WriterThread writerThread = new WriterThread(serialBuffer, bigBuffer2, true);
        writerThread.start();

        byte[] dataReceived = serialBuffer.getWriteBuffer();
        Assert.assertArrayEquals(bigBuffer2, dataReceived);
    }

    private class WriterThread extends Thread{

        private final static long DELAY_TIME = 5000;

        private SerialBuffer serialBuffer;
        private byte[] data;
        private boolean delay;

        public WriterThread(SerialBuffer serialBuffer, byte[] data, boolean delay){
            this.serialBuffer = serialBuffer;
            this.data = data;
            this.delay = delay;
        }

        @Override
        public void run() {
            if(delay)
                delayWrite();
            serialBuffer.putWriteBuffer(data);
        }

        private void delayWrite(){
            synchronized (this) {
                try {
                    wait(DELAY_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
