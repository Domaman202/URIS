package ru.uris.gadapter


import ru.uris.RemoteMethod

trait GroovySocket {
    Object invokeRemoteMethod(RemoteMethod method, Object... args) throws IOException {
        return this.getObjectPool().get(method.obj).invokeMethod(method.name, args)
    }

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
