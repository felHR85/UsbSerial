package com.felhr.utils;

import com.annimon.stream.IntStream;
import com.annimon.stream.function.IntPredicate;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Thanks to Thomas Moorhead for improvements and suggestions

public class ProtocolBuffer {

    public static final String BINARY = "binary";
    public static final String TEXT = "text";

    private String mode;

    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

    private byte[] rawBuffer;
    private int bufferPointer = 0;

    private byte[] separator;
    private String delimiter;
    private StringBuilder stringBuffer;

    private List<String> commands = new ArrayList<>();
    private List<byte[]> rawCommands = new ArrayList<>();

    public ProtocolBuffer(String mode){
        this.mode = mode;
        if(mode.equals(BINARY)){
            rawBuffer = new byte[DEFAULT_BUFFER_SIZE];
        }else{
            stringBuffer = new StringBuilder(DEFAULT_BUFFER_SIZE);
        }
    }

    public ProtocolBuffer(String mode, int bufferSize){
        this.mode = mode;
        if(mode.equals(BINARY)){
            rawBuffer = new byte[bufferSize];
        }else{
            stringBuffer = new StringBuilder(bufferSize);
        }
    }

    public void setDelimiter(String delimiter){
        this.delimiter = delimiter;
    }

    public void setDelimiter(byte[] delimiter){
        this.separator = delimiter;
    }

    public synchronized void appendData(byte[] data){
        // Ignore the frequent empty calls
        if (data.length == 0) return;

        if(mode.equals(TEXT)){
            try {
                String dataStr = new String(data, "UTF-8");
                stringBuffer.append(dataStr);

                String buffer = stringBuffer.toString();
                int prevIndex = 0;
                int index = buffer.indexOf(delimiter);
                while (index >= 0) {
                    String tempStr = buffer.substring(prevIndex, index + delimiter.length());
                    commands.add(tempStr);
                    prevIndex = index + delimiter.length();
                    index = stringBuffer.toString().indexOf(delimiter, prevIndex);
                }

                if( /*prevIndex < buffer.length() &&*/ prevIndex > 0){
                    String tempStr = buffer.substring(prevIndex, buffer.length());
                    stringBuffer.setLength(0);
                    stringBuffer.append(tempStr);
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }else if(mode.equals(BINARY)){
            appendRawData(data);
        }
    }

    public boolean hasMoreCommands(){
        if(mode.equals(TEXT)) {
            return commands.size() > 0;
        }else {
            return rawCommands.size() > 0;
        }
    }

    public String nextTextCommand(){
        if(commands.size() > 0){
            return commands.remove(0);
        }else{
            return null;
        }
    }

    public byte[] nextBinaryCommand(){
        if(rawCommands.size() > 0){
            return rawCommands.remove(0);
        }else{
            return null;
        }
    }

    private void appendRawData(byte[] rawData){

        System.arraycopy(rawData, 0, rawBuffer, bufferPointer, rawData.length);
        bufferPointer += rawData.length;

        SeparatorPredicate predicate = new SeparatorPredicate();
        int[] indexes =
                IntStream.range(0, bufferPointer)
                        .filter(predicate)
                        .toArray();

        int prevIndex = 0;
        for(Integer i : indexes){
            byte[] command = Arrays.copyOfRange(rawBuffer, prevIndex, i + separator.length);
            rawCommands.add(command);
            prevIndex = i + separator.length;
        }

        if(prevIndex < rawBuffer.length
                && prevIndex > 0){
            byte[] tempBuffer = Arrays.copyOfRange(rawBuffer, prevIndex, rawBuffer.length);
            bufferPointer = 0;
            System.arraycopy(tempBuffer, 0, rawBuffer, bufferPointer, rawData.length);
            bufferPointer += rawData.length;
        }

    }

    private class SeparatorPredicate implements IntPredicate{
        @Override
        public boolean test(int value) {
            if(rawBuffer[value] == separator[0]){
                for(int i=1;i<=separator.length-1;i++){
                    if(rawBuffer[value + i] != separator[i]){
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }
}
