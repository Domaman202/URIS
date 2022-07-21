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

    public static Type ofId(int id) {
        return switch (id) {
            case 5 -> BYTE;
            case 4 -> SHORT;
            case 3 -> INT;
            case 2 -> LONG;
            case 1 -> DOUBLE;
            case 0 -> STRING;
            case -1 -> OBJECT;
            case -2 -> NULL;
            default -> throw new RuntimeException("Invalid id!");
        };
    }

    public int getId() {
        return switch (this) {
            case BYTE -> 5;
            case SHORT -> 4;
            case INT -> 3;
            case LONG -> 2;
            case DOUBLE -> 1;
            case STRING -> 0;
            case OBJECT -> 8;
            case NULL -> 9;
        };
    }

    public static Type of(Object obj) {
        if (obj == null)
            return Type.NULL;
        return Type.of(obj.getClass());
    }

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
