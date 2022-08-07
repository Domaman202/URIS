package ru.DmN;

import java.util.function.Supplier;

public class Lazy <T> {
    public final Supplier<T> supplier;
    public T instance;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (instance == null)
            return instance = supplier.get();
        return instance;
    }
}
