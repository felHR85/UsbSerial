package com.felhr.utils;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProtocolBuffer {

    public static final String BINARY = "binary.";
    public static final String TEXT = "text";

    /**
     * REGEX
     * .+?(?<=\\r\\n)
     */

    private final String baseRegex1 = "(.+?(?<=";
    private final String baseRegex2 = "))";

    private String mode;

    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

    private byte[] rawBuffer;
    private byte[] separator;
    private StringBuilder stringBuffer;

    private List<String> commands = new ArrayList<>();

    private Pattern wholeCommandPattern;

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

    public void setTrailChars(String trailChars){
        wholeCommandPattern = Pattern.compile(baseRegex1 + adaptTrailChars(trailChars) + baseRegex2,
                Pattern.MULTILINE);
    }

    public void setSeparator(byte[] separator){
        this.separator = separator;
    }

    public void setRegex(String regex){
        wholeCommandPattern = Pattern.compile(regex);
    }

    public void appendData(byte[] data){
        if(mode.equals(TEXT)){
            try {
                String dataStr = new String(data, "UTF-8");
                stringBuffer.append(dataStr);

                Matcher matcher1 = wholeCommandPattern.matcher(stringBuffer.toString());
                if(matcher1.find()) {
                    int groupCount = matcher1.groupCount();

                    for (int i = 0; i <= groupCount - 1; i++) {
                        commands.add(matcher1.group(i));
                    }

                    stringBuffer.replace(0, stringBuffer.length(), matcher1.replaceAll(""));
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

    private String adaptTrailChars(String trailChars){
        String tempStr = trailChars;
        if(trailChars.contains("\r\n")){
            tempStr = tempStr.replace("\r\n", "\\\\r\\\\n");
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
