package ru.uris;

public class RemoteMethod {
    public final String name;
    public final ARType[] args;
    public final ARType ret;

    public RemoteMethod(String name, ARType[] args, ARType ret) {
        this.name = name;
        this.args = args;
        this.ret = ret;
    }
}
