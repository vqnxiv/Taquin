package io.github.vqnxiv.taquin.util;


import java.lang.reflect.InvocationTargetException;
import java.util.Optional;


public class Utils {
    
    // genericFieldOrMethodName -> Generic field or method name
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
    
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> staticMethodReflectionCall(Class <?> clazz, String methodName, Class<T> returnType) {
        try {
            return Optional.ofNullable((T) clazz.getMethod(methodName).invoke(null));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        
        return Optional.empty();
    }
}
