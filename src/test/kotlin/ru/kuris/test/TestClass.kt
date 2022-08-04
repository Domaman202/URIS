package ru.kuris.test

class TestClass(@JvmField val i: Int) : IAdder {
    override fun add(j: Int): Int {
        return i + j
    }

    fun print(value: Any?) {
        println(value)
    }
}