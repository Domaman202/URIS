package ru.uris.gadapter

trait GroovySocket {
    synchronized <T extends Enum<T>> T readEnum(Class<T> clazz) throws IOException {
        this.checkValue(6);
        var name = this.readStringI();
        try {
            return Arrays.stream((T[]) clazz.values()).filter(o -> o.name() == name).findFirst().orElseThrow();
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }
}
