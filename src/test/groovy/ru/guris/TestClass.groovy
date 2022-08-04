package ru.guris

record TestClass(int i) implements IAdder {
    @Override
    int add(int j) {
        return i + j;
    }

    void print(Object value) {
        System.out.println(value);
    }
}