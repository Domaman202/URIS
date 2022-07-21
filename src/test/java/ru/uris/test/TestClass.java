package ru.uris.test;

public record TestClass(int i) implements IAdder {
    @Override
    public int add(int j) {
        return i + j;
    }

    public void print(Object value) {
        System.out.println(value);
    }
}
