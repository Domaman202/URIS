package ru.kuris

import kotlin.reflect.KClass

class ARType(@JvmField val dim: Int, @JvmField val type: PType) {
    override fun toString(): String {
        return "<$type[$dim]>"
    }

    companion object {
        @JvmStatic
        fun of(obj: Any?): ARType {
            if (obj == null)
                return ARType(0, PType.NULL)
            return of(obj::class)
        }

        @JvmStatic
        fun of(clazz: KClass<out Any>): ARType {
            if (clazz.java.isArray)
                return ARType(1, PType.of(clazz.java.arrayType().kotlin)) // TODO: динамическое вычисление кол-ва элементов
            return ARType(0, PType.of(clazz))
        }
    }
}