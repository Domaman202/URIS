package ru.uris;

public enum Type {
    BYTE,
    SHORT,
    INT,
    LONG,
    DOUBLE,
    STRING,
    OBJECT,
    NULL;

    public static Type of(Class<?> clazz) {
        if (clazz == null)
            return NULL;
        if (clazz == Boolean.TYPE || clazz == Boolean.class || clazz == Byte.TYPE || clazz == Byte.class)
            return BYTE;
        if (clazz == Short.TYPE || clazz == Short.class || clazz == Character.TYPE || clazz == Character.class)
            return SHORT;
        if (clazz == Integer.TYPE || clazz == Integer.class)
            return INT;
        if (clazz == Long.TYPE || clazz == Long.class)
            return LONG;
        if (clazz == Float.TYPE || clazz == Float.class || clazz == Double.TYPE || clazz == Double.class)
            return DOUBLE;
        if (clazz == String.class)
            return STRING;
        return OBJECT;
    }
}
