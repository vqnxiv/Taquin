package io.github.vqnxiv.taquin.util;


import java.lang.reflect.InvocationTargetException;


public class Utils {
    
    
    public static String toReadable(String s) {
        
        if(s == null) return "NULL STRING";
        if(s.equals("")) return "EMPTY STRING";
        
        var sb = new StringBuilder();

        String[] splitString = s.split("(?=\\p{Upper})");
        
        sb.append(splitString[0].substring(0, 1).toUpperCase()).append(splitString[0].substring(1));
        
        for(int i = 1; i < splitString.length; i++) {
            sb.append(' ');
            sb.append(splitString[i].toLowerCase());
        }
        
        return sb.toString();
    }
    
    
    public static boolean getBooleanMethodReturn(Class<?> clazz, String methodName) {
        try {
            return (boolean) clazz.getMethod(methodName).invoke(null); 
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    public static int getIntMethodReturn(Class<?> clazz, String methodName) {
        try {
            return (int) clazz.getMethod(methodName).invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static String getStringMethodReturn(Class<?> clazz, String methodName) {
        try {
            return (String) clazz.getMethod(methodName).invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }
}
