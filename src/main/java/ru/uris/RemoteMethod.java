package ru.uris;

public class RemoteMethod {
    public final String name;
    public final ARType[] args;
    public final ARType ret;
    public final int obj;

    public RemoteMethod(String name, ARType[] args, ARType ret, int obj) {
        this.name = name;
        this.args = args;
        this.ret = ret;
        this.obj = obj;
    }
}
