package ru.uris;

public enum PType {
    STRING(0),
    DOUBLE(1),
    LONG(2),
    INT(3),
    SHORT(4),
    BYTE(5),
    ENUM(6),
    PACKET(7),
    OBJECT(8),
    NULL(9);

    public final int id;

    PType(int id) {
        this.id = id;
    }

    public static PType of(Class<?> clazz) {
        if (clazz == String.class)
            return STRING;
        if (clazz == Float.TYPE || clazz == Float.class || clazz == Double.TYPE || clazz == Double.class)
            return DOUBLE;
        if (clazz == Long.TYPE || clazz == Long.class)
            return LONG;
        if (clazz == Integer.TYPE || clazz == Integer.class)
            return INT;
        if (clazz == Short.TYPE || clazz == Short.class || clazz == Character.TYPE || clazz == Character.class)
            return SHORT;
        if (clazz == Boolean.TYPE || clazz == Boolean.class || clazz == Byte.TYPE || clazz == Byte.class)
            return BYTE;
        if (Enum.class.isAssignableFrom(clazz))
            return ENUM;
        if (Packet.class.isAssignableFrom(clazz))
            return PACKET;
        if (clazz == Void.TYPE || clazz == Void.class)
            return NULL;
        return OBJECT;
    }
}
