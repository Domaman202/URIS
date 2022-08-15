package ru.uris.test.main;

public record TestClass(int i) implements INumber {
    @Override
    public INumber add(int j) {
        return new TestClass(i + j);
    }

    @Override
    public INumber add(INumber j) {
        return new TestClass(i + j.toInt());
    }

    @Override
    public int toInt() {
        return i;
    }

    @Override
    public String toString() {
        return "TestClass(" + i + ")";
    }
}
