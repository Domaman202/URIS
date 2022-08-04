package ru.DmN;

import java.lang.reflect.Method;

public class ReflectionUtils {
    public static Method[] getAllMethods(Class<?> clazz) {
        var methods0 = clazz.getMethods();
        var methods1 = clazz.getDeclaredMethods();
        var methods = new Method[methods0.length + methods1.length];
        System.arraycopy(methods0, 0, methods, 0, methods0.length);
        System.arraycopy(methods1, 0, methods, methods0.length, methods1.length);
        return methods;
    }
}
