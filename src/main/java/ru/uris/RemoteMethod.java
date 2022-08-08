package ru.uris;

import java.lang.reflect.Method;

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

    public boolean equals(Method method) {
        if (method.getName().equals(this.name)) {
            if (ARType.of(method.getReturnType()).equals(this.ret)) {
                if (method.getParameterCount() == this.args.length) {
                    var i = 0;
                    for (int j = 0; j < this.args.length; j++)
                        if (ARType.of(method.getParameterTypes()[j]).equals(this.args[j]))
                            i++;
                    return i == this.args.length;
                }
            }
        }
        return false;
    }
}
