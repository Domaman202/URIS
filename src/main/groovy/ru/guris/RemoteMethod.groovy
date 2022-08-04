package ru.guris

class RemoteMethod {
    final String name
    final ARType[] args
    final ARType ret

    RemoteMethod(String name, ARType[] args, ARType ret) {
        this.name = name;
        this.args = args;
        this.ret = ret;
    }
}
