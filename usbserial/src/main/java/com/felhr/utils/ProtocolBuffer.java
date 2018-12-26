package com.felhr.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProtocolBuffer {

    public static final String BINARY = "binary";
    public static final String TEXT = "text";

    private final String baseRegex1 = "(?<=";
    private final String baseRegex2 = ")";

    private String mode;

    private String finalRegex;

    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

    private String trailChars;
    private String regex;

    private byte[] rawBuffer;
    private StringBuilder stringBuffer;

    private List<String> commands = new ArrayList<>();

    public ProtocolBuffer(String mode){
        this.mode = mode;
        if(mode.equals(BINARY)){
            rawBuffer = new byte[DEFAULT_BUFFER_SIZE];
        }else{
            stringBuffer = new StringBuilder(DEFAULT_BUFFER_SIZE);
        }
    }

    public ProtocolBuffer(String mode, int bufferSize){
        if(mode.equals(BINARY)){
            rawBuffer = new byte[bufferSize];
        }else{
            stringBuffer = new StringBuilder(bufferSize);
        }
    }

    public void setTrailChars(String trailChars){
        finalRegex = baseRegex1 + adaptTrailChars(trailChars) + baseRegex2;
    }

    public void setRegex(String regex){
        finalRegex = regex;
    }

    public void appendData(byte[] data){
        if(mode.equals(TEXT)){
            try {
                String dataStr = new String(data, "UTF-8");
                stringBuffer.append(dataStr);
                String[] splitStr = stringBuffer.toString().split(finalRegex);
                if(splitStr.length > 1){
                    commands.addAll(Arrays.asList(splitStr));
                    stringBuffer.setLength(0);
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private String adaptTrailChars(String trailChars){
        String tempStr = trailChars;

        if(trailChars.contains("\\")){
            tempStr = tempStr.replace("\\", "\\\\");
        }

        if(trailChars.contains(".")){
            tempStr = tempStr.replace(".", "\\.");
        }

        if(trailChars.contains("+")){
            tempStr = tempStr.replace("+", "\\+");
        }

        if(trailChars.contains("*")){
            tempStr = tempStr.replace("*", "\\*");
        }

        if(trailChars.contains("?")){
            tempStr = tempStr.replace("?", "\\?");
        }

        if(trailChars.contains("[")){
            tempStr = tempStr.replace("[", "\\[");
        }

        if(trailChars.contains("^")){
            tempStr = tempStr.replace("^", "\\^");
        }

        if(trailChars.contains("]")){
            tempStr = tempStr.replace("]", "\\]");
        }

        if(trailChars.contains("$")){
            tempStr = tempStr.replace("$", "\\$");
        }

        if(trailChars.contains("(")){
            tempStr = tempStr.replace("(", "\\(");
        }

        if(trailChars.contains(")")){
            tempStr = tempStr.replace(")", "\\)");
        }

        if(trailChars.contains("{")){
            tempStr = tempStr.replace("{", "\\{");
        }

        if(trailChars.contains("}")){
            tempStr = tempStr.replace("}", "\\}");
        }

        if(trailChars.contains("=")){
            tempStr = tempStr.replace("=", "\\=");
        }

        if(trailChars.contains("!")){
            tempStr = tempStr.replace("!", "\\!");
        }

        if(trailChars.contains("<")) {
            tempStr = tempStr.replace("<", "\\<");
        }

        if(trailChars.contains(">")){
            tempStr = tempStr.replace(">", "\\>");
        }

        if(trailChars.contains("|")){
            tempStr = tempStr.replace("|", "\\|");
        }

        if(trailChars.contains(":")){
            tempStr = tempStr.replace(":", "\\:");
        }

        if(trailChars.contains("-")){
            tempStr = tempStr.replace("-", "\\-");
        }

        if(trailChars.contains("/")){
            tempStr = tempStr.replace("/", "\\/");
        }

        return tempStr;
    }

}
