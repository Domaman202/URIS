package ru.uris.test;

import ru.uris.Client;
import ru.uris.ObjectServer;
import ru.uris.Type;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TestClient {
    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        try (var client = new Client("localhost", 2022)) {
            client.runListener();

            var obj = client.getRemoteObject(0);
            var result = obj.invokeMethod("add", new Type[]{Type.INT}, 21);
            System.out.println(result);
            obj.invokeMethod("print", new Type[]{Type.OBJECT}, (Object) null);
            obj.invokeMethod("print", new Type[]{Type.OBJECT}, "Hello, World!");
            obj.invokeMethod("print", new Type[]{Type.OBJECT}, 12.21);

            var obj0 = (IAdder) client.getRemoteObject(0, IAdder.class);
            var result0 = obj0.add(21);
            System.out.println(result0);
        }
    }
}
