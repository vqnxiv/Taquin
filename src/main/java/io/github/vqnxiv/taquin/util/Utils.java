package io.github.vqnxiv.taquin.util;


import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


/**
 * Non instantiable general utility class which only contains static final methods.
 */
public final class Utils {

    /**
     * Can't be instantiated.
     */
    private Utils() {}

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
     * @return {@link Optional#of(Object)} the return value if the method was successfully invoked;
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
     * @return {@link Optional#of(Object)} the return value if the method was successfully invoked;
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
     * @return {@link Optional#of(Object)} the field if it was successfully accessed;
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
     * @return {@link Optional#of(Object)} the field if it was successfully accessed;
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

    /**
     * Utility method to compare two nested int arrays of the same dimensions.
     * <p>
     * This method does no verification and should not be called if the arrays do not
     * have the same dimensions or are not {@code int[]} at their most nested level.
     * <p>
     * The actual objective of this method is that it must return {@code 0} if two 
     * arrays are such that {@code tab1.equalsTo(tab2) == true} and that
     * the results are transitive: if the method determines that {@code tab1 < tab2} 
     * and {@code tab2 < tab 3}, then it must return {@code tab1 < tab3}.
     * <p>
     * While it needs not return consistent results across runtimes, it must remain
     * consistent through a single runtime.
     * 
     * @param t1 The first array to compare, as in {@code t1.compareTo(t2)}.
     * @param t2 The second array to compare, as in {@code t1.compareTo(t2)}.
     * @return int value of the comparison.
     * @throws UnsupportedOperationException if either condition breaks.
     */
    public static int intArrayDeepCompare(Object[] t1, Object[] t2) throws UnsupportedOperationException {
        if(t1 == t2) {
            return 0;
        }

        if(t1 == null || t2 == null) {
            return (t1 == null) ? -1 : 1;
        }
        
        int minLength = Math.min(t1.length, t2.length);
        
        for(int i = 0; i < minLength; i++) {
            Object e1 = t1[i];
            Object e2 = t2[i];
            
            if(e1 != e2) {
                if(e1 == null || e2 == null) {
                    return (e1 == null) ? -1 : 1;
                }
                
                int v = intOrArrayCompare(e1, e2);
                if(v != 0) {
                    return v;
                }
            }
        }
        
        return t1.length - t2.length;
    }

    /**
     * Helper method for {@link #intArrayDeepCompare(Object[], Object[])}.
     * <p>
     * Either calls {@link Arrays#compare(int[], int[])} or {@link #intArrayDeepCompare(Object[], Object[])}
     * depending on the class of the {@code a} and {@code b}.
     * 
     * @param a The first object to compare.
     * @param b The second object to compare.
     * @return {@link Arrays#compare(int[], int[])} if {@code a} and {@code b}
     * are {@code int[]}; {@link #intArrayDeepCompare(Object[], Object[])}
     * if {@code a} and {@code b} are {@code Object[]}. 
     * @throws UnsupportedOperationException otherwise.
     */
    private static int intOrArrayCompare(Object a, Object b) {
        if(a instanceof int[] intA && b instanceof int[] intB) {
            return Arrays.compare(intA, intB);
        }
        else if (a instanceof Object[] objA && b instanceof Object[] objB) {
            return intArrayDeepCompare(objA, objB);
        }
        else {
            throw new UnsupportedOperationException("Object from different classes.");
        }
    }
}
