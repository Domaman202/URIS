package ru.kuris

import kotlin.reflect.KClass

enum class PType(id: Int) {
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

    companion object {
        @JvmStatic
        fun of(clazz: KClass<out Any>): PType {
            return when (clazz) {
                String::class -> STRING
                Double::class, Float::class -> DOUBLE
                Long::class -> LONG
                Int::class -> INT
                Short::class -> SHORT
                Byte::class -> BYTE
                Unit::class, Void::class -> NULL
                else -> {
                    if (Enum::class.java.isInstance(clazz.java))
                        ENUM
                    if (Packet::class.java.isInstance(clazz.java))
                        PACKET
                    OBJECT
                }
            }
        }
    }
}