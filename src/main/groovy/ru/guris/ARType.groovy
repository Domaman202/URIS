package ru.guris

class ARType {
    final int dim;
    final PType type;

    protected ARType(int dim, PType type) {
        this.dim = dim
        this.type = type
    }

    static ARType of(Object obj) {
        if (obj == null)
            return new ARType(0, PType.NULL)
        return of(obj.class)
    }

    static ARType of(Class<?> clazz) {
        if (clazz.isArray())
            return new ARType(1, PType.of(clazz.arrayType())) // TODO: динамическое вычисление кол-ва элементов
        return new ARType(0, PType.of(clazz))
    }

    @Override
    String toString() {
        return "<${this.type}[${this.dim}]>"
    }
}