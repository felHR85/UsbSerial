package com.felhr.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ProtocolBuffer {

    public static final String BINARY = "binary.";
    public static final String TEXT = "text";

    private String mode;

    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

    private byte[] rawBuffer;

    private byte[] separator;
    private String delimiter;
    private StringBuilder stringBuffer;

    private List<String> commands = new ArrayList<>();

    private String regex;

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

    public void setSeparator(byte[] separator){
        this.separator = separator;
    }

    public void appendData(byte[] data){
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

                if(prevIndex < buffer.length()
                        && prevIndex > 0){
                    String tempStr = buffer.substring(prevIndex, buffer.length());
                    stringBuffer.setLength(0);
                    stringBuffer.append(tempStr);
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }else if(mode.equals(BINARY)){
            //TODO:!!
        }
    }

    public boolean hasMoreCommands(){
        return commands.size() > 0;
    }

    public String nextCommand(){
        if(commands.size() > 0){
            return commands.remove(0);
        }else{
            return null;
        }
    }
}
