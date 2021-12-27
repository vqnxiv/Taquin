package io.github.vqnxiv.taquin.util;


import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;


public final class Utils {
    
    private Utils() {}

    public static final StringConverter<Integer> intStringConverter = new StringConverter<>() {
        @Override
        public String toString(Integer n) {
            return (n != null) ? Integer.toString(n) : "0";
        }

        @Override
        public Integer fromString(String string) {
            return (!string.equals("")) ? Integer.parseInt(string) : 0;
        }
    };

    public static final StringConverter<Class<?>> clsStringConverter = new StringConverter<>() {
        @Override
        public String toString(Class clazz) {
            return (clazz != null) ? clazz.getSimpleName() : "";
        }

        @Override
        public Class<?> fromString(String string) {
            return null;
        }
    };

    public static final StringConverter<Class<?>> srchClsConv = new StringConverter<>() {
        @Override
        public String toString(Class<?> srchCls) {
            return (String) Utils.staticFieldGet(srchCls, "SEARCH_SHORT_NAME").orElse("");
        }

        @Override
        public Class<?> fromString(String string) {
            return null;
        }
    };
    
    public static final UnaryOperator<TextFormatter.Change> integerFilter = change -> {
        String input = change.getText();
        if (input.matches("[0-9]*")) {
            return change;
        }
        return null;
    };
    
    
    // CONSTANT_OR_ENUM_NAME -> Constant or enum name
    public static String constantToReadable(String s) {

        if(s == null) throw new NullPointerException();
        if(s.equals("")) return s;
        
        var sb = new StringBuilder();
        
        String[] splitString = s.split("_");

        sb.append(splitString[0].substring(0, 1).toUpperCase()).append(splitString[0].substring(1).toLowerCase());

        for(int i = 1; i < splitString.length; i++) {
            sb.append(' ');
            sb.append(splitString[i].toLowerCase());
        }

        return sb.toString();
    }
    
    // genericFieldOrMethodName -> Generic field or method name
    public static String camelToReadable(String s) {
        
        if(s == null) throw new NullPointerException();
        if(s.equals("")) return s;
        
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
    public static <T> Optional<T> staticMethodCall(Class<?> clazz, String methodName) {
        try {
            return Optional.of((T) clazz.getMethod(methodName).invoke(null));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        
        return Optional.empty();
    }

    // List<T> instead of Class<T> so we can cast to a generic type
    // as we can't get .class from a generic
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> staticMethodCallAndCast(Class<?> clazz, String methodName, List<T> doNotRemove) {
        try {
            return Optional.of((T) clazz.getMethod(methodName).invoke(null));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> staticFieldGet(Class<?> clazz, String fieldName) {
        try {
            return Optional.of((T) clazz.getField(fieldName).get(null));
        } catch(NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> staticFieldGetAndCast(Class<?> clazz, String fieldName, List<T> doNotRemove) {
        try {
            return Optional.of((T) clazz.getField(fieldName).get(null));
        } catch(NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }
}
