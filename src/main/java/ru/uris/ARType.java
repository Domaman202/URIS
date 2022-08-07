package ru.uris;

public class ARType {
    public final int dim;
    public final PType type;

    protected ARType(int dim, PType type) {
        this.type = type;
        this.dim = dim;
    }

    public static ARType of(Object obj) {
        if (obj == null)
            return new ARType(0, PType.NULL);
        return ARType.of(obj.getClass());
    }

    public static ARType of(Class<?> clazz) {
        if (clazz.isArray())
            return new ARType(1, PType.of(clazz.arrayType())); // TODO: динамическое вычисление кол-ва элементов
        return new ARType(0, PType.of(clazz));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ARType type)
            return this.dim == type.dim && this.type == type.type;
        return false;
    }

    @Override
    public String toString() {
        return "<" + this.type + "[" + this.dim + "]" + ">";
    }
}
