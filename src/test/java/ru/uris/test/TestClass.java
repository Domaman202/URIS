package ru.uris.test;

public record TestClass(int i) implements IAdder, Cloneable {
    @Override
    public int add(int j) {
        return i + j;
    }

    @Override
    protected TestClass clone() throws CloneNotSupportedException {
        return (TestClass) super.clone();
    }
}
