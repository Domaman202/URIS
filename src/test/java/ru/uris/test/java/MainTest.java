package ru.uris.test.java;

import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.uris.Client;
import ru.uris.Packet;
import ru.uris.RemoteMethod;
import ru.uris.Server;

import java.io.IOException;
import java.util.Arrays;

public class MainTest {
    public static final Logger SLOG = LoggerFactory.getLogger("Server");
    public static final Logger CLOG = LoggerFactory.getLogger("Client");
    public static volatile boolean flag;

    public static void main(String[] args) throws InterruptedException, IOException {
        var serverThread = new Thread(() -> {
            // Server
            try (var server = new Server(2014)) {
                server.objectPool.add(new Object());
                server.objectPool.add(new TestClass(12));
                server.objectPool.add(MainTest.class.getClassLoader());

                // Set client flag
                flag = true;
                //
                var connection = server.accept();
                connection.createListener().start();

                SLOG.info("{}\n{}",
                        Arrays.toString(((Packet.PObjectList) connection.sendAndReceive(new Packet.PObjectList(Packet.nextId()))).objects),
                        Arrays.toString(((Packet.PMethodList) connection.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods)
                );

                RemoteMethod toString = null;
                for (var method : ((Packet.PMethodList) connection.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods) {
                    SLOG.info("[NAME]{}\n[ARGS]:{}\n[RETURN]:{}", method.name, Arrays.toString(method.args), method.ret);
                    if (method.name.equals("toString"))
                        toString = method;
                }

//                SLOG.info("{}", connection.sendAndReceive(new Packet.PMethodCall(Packet.nextId(), toString)));
            } catch (Throwable e) {
                throw new Error(e);
            }
        });

        var clientThread = new Thread(() -> {
            // Wait server
            while (!flag)
                Thread.onSpinWait();
            // Client
            try (var client = new Client("localhost", 2014)) {
                client.objectPool.add(new Object());

                client.createListener().start();

                CLOG.info("{}\n{}",
                        Arrays.toString(((Packet.PObjectList) client.sendAndReceive(new Packet.PObjectList(Packet.nextId()))).objects),
                        Arrays.toString(((Packet.PMethodList) client.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods)
                );

                RemoteMethod toString = null;
                for (var method : ((Packet.PMethodList) client.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods) {
                    SLOG.info("[NAME]{}\n[ARGS]:{}\n[RETURN]:{}", method.name, Arrays.toString(method.args), method.ret);
                    if (method.name.equals("toString"))
                        toString = method;
                }

                CLOG.info("{}", client.sendAndReceive(new Packet.PMethodCall(Packet.nextId(), toString)));
            } catch (Throwable e) {
                throw new Error(e);
            }
        });

        org.apache.log4j.Logger.getRootLogger().addAppender(new FileAppender(new PatternLayout("%r [%t] %p %c %x\n%m%n\n"), "Java_MainTest.log"));

        serverThread.start();
        clientThread.start();

        serverThread.join(5000);
        clientThread.join(5000);
    }
}
