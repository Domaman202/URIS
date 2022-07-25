package ru.uris;

public class RemoteMethod {
    public final String name;
    public final VType[] args;
    public final VType ret;

    public RemoteMethod(String name, VType[] args, VType ret) {
        this.name = name;
        this.args = args;
        this.ret = ret;
    }
}
