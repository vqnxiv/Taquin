package io.github.vqnxiv.taquin.util;


import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;


/**
 * Non instantiable general utility class which only contains static final methods.
 */
public final class Utils {
    
    
    private Utils() {}


    /**
     * {@link StringConverter} which converts between {@link String} and {@link Integer}
     */
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

    /**
     * {@link StringConverter} which converts returns {@link Class#getSimpleName()} from a {@code .class} object
     * and {@link Class#forName(String)} from a {@link String}, or {@code null} if {@link ClassNotFoundException}
     */
    public static final StringConverter<Class<?>> clsStringConverter = new StringConverter<>() {
        @Override
        public String toString(Class clazz) {
            return (clazz != null) ? clazz.getSimpleName() : "";
        }

        @Override
        public Class<?> fromString(String string) {
            try {
                return Class.forName(string);
            } catch(ClassNotFoundException e) {
                return null;
            }
        }
    };

    /**
     * {@link StringConverter} which converts returns {@code Search#name} from a {@code .class} object from
     * {@link io.github.vqnxiv.taquin.solver.Search} or one of its subclasses,
     * and {@link Class#forName(String)} from a {@link String}, or {@code null} if {@link ClassNotFoundException}
     */
    public static final StringConverter<Class<?>> srchClsConv = new StringConverter<>() {
        @Override
        public String toString(Class<?> srchCls) {
            return (String) Utils.staticFieldGet(srchCls, "SEARCH_SHORT_NAME").orElse("");
        }

        @Override
        public Class<?> fromString(String string) {
            try {
                return Class.forName(string);
            } catch(ClassNotFoundException e) {
                return null;
            }
        }
    };

    /**
     * {@link UnaryOperator} which filters out non digit characters from a {@link String}
     */
    public static final UnaryOperator<TextFormatter.Change> integerFilter = change -> {
        String input = change.getText();
        if (input.matches("[0-9]*")) {
            return change;
        }
        return null;
    };


    /**
     * Utility method which converts a {@link String} from SCREAMING_SNAKE_CASE to normal case
     * <p>
     * e.g CONSTANT_OR_ENUM_NAME -> Constant or enum name
     * @param s The {@link String} to convert
     * @return the converted {@link String}
     */
    public static String screamingSnakeToReadable(String s) {

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

    /**
     * Utility method which converts a {@link String} from camelCase to normal case
     * <p>
     * e.g genericFieldOrMethodName -> Generic field or method name
     * @param s The {@link String} to convert
     * @return the converted {@link String}
     */
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

    /**
     * Calls a static method from a given class.
     * 
     * @param clazz The class which contains the method.
     * @param methodName The name of the static method which should be invoked.
     * @param <T> The return type of the method.
     * @return {@link Optional#of(T)} the return value if the method was successfully invoked;
     * {@link Optional#empty()} otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> staticMethodCall(Class<?> clazz, String methodName) {
        try {
            return Optional.of((T) clazz.getMethod(methodName).invoke(null));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        
        return Optional.empty();
    }

    /**
     * Calls a static method from a given class and 'casts' its return to the type of the given {@link List}.
     * <p>
     * This is used when working with a generic return type, e.g {@link Enum#valueOf(Class, String)} or {@code values}.
     *
     * @param clazz The class which contains the method.
     * @param methodName The name of the static method which should be invoked.
     * @param doNotRemove a {@link List} of the type which the return value should be cast to.
     * @param <T> The return type of the method.
     * @return {@link Optional#of(T)} the return value if the method was successfully invoked;
     * {@link Optional#empty()} otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> staticMethodCallAndCast(Class<?> clazz, String methodName, List<T> doNotRemove) {
        try {
            return Optional.of((T) clazz.getMethod(methodName).invoke(null));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Calls a static method from a given class.
     *
     * @param clazz The class which contains the field.
     * @param fieldName The name of the static field which should be returned.
     * @param <T> The type of the field.
     * @return {@link Optional#of(T)} the field if it was successfully accessed;
     * {@link Optional#empty()} otherwise
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> staticFieldGet(Class<?> clazz, String fieldName) {
        try {
            return Optional.of((T) clazz.getField(fieldName).get(null));
        } catch(NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Calls a static method from a given class and 'casts' its return to the type of the given {@link List}.
     * <p>
     * This is used when working with a generic type.
     *
     * @param clazz The class which contains the field.
     * @param fieldName The name of the static field which should be returned.
     * @param doNotRemove a {@link List} of the type which the field value should be cast to.
     * @param <T> The type of the field.
     * @return {@link Optional#of(T)} the field if it was successfully accessed;
     * {@link Optional#empty()} otherwise
     */
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
