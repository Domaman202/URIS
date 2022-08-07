package ru.uris.test.main;

public record TestClass(int i) implements INumber {
    @Override
    public INumber add(int j) {
        return new TestClass(i + j);
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
