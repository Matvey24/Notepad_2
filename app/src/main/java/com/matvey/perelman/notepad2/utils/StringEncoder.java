package com.matvey.perelman.notepad2.utils;

public class StringEncoder {
    private static final StringBuilder sb = new StringBuilder();
    public static String encode(String s){
        if(s.isEmpty())
            return s;
        sb.ensureCapacity(s.length() * 2);
        for(int i = 0; i < s.length(); ++i){
            char c = s.charAt(i);
            switch (c){
                case '\'':
                    sb.append("\\s");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append(c);
            }
        }
        s = sb.toString();
        sb.setLength(0);
        return s;
    }
    public static String decode(String s){
        if(s == null || s.isEmpty())
            return s;
        sb.ensureCapacity(s.length());
        for(int i = 0; i < s.length(); ++i){
            char c = s.charAt(i);
            if(c == '\\'){
                ++i;
                c = s.charAt(i);
                switch (c){
                    case '\\':
                        sb.append("\\");
                        break;
                    case 's':
                        sb.append("'");
                        break;
                }
            }else{
                sb.append(c);
            }
        }
        s = sb.toString();
        sb.setLength(0);
        return s;
    }
}
