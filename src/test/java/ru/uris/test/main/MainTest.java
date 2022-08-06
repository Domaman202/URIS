package ru.uris.test.main;

import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.uris.*;
import ru.uris.test.java.TestImpl;

import java.io.IOException;
import java.util.Arrays;

public class MainTest {
    public static final Logger SLOG = LoggerFactory.getLogger("Server");
    public static final Logger CLOG = LoggerFactory.getLogger("Client");
    public static volatile boolean flag;

    public static void main(ServerSocketSupplier ss, ClientSocketSupplier cs, String name) throws IOException, InterruptedException {
        org.apache.log4j.Logger.getRootLogger().addAppender(new FileAppender(new PatternLayout("%r [%t] %p %c %x\n%m%n\n"), name + ".log"));

        var serverThread = new Thread(() -> {
            // Server
            try (var server = ss.supply(2014)) {
                server.objectPool.add(new Object());
                server.objectPool.add(new TestClass(12));
                server.objectPool.add(TestImpl.class.getClassLoader());

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

                SLOG.info("{}", ((Packet.PMethodCall) connection.sendAndReceive(new Packet.PMethodCall(Packet.nextId(), toString))).result);
            } catch (Throwable e) {
                throw new Error(e);
            }
        });

        var clientThread = new Thread(() -> {
            // Wait server
            while (!flag)
                Thread.onSpinWait();
            // Client
            try (var client = cs.supply("localhost", 2014)) {
                client.objectPool.add(new Object());
                MainTest.test(client, CLOG);
            } catch (Throwable e) {
                throw new Error(e);
            }
        });

        serverThread.start();
        clientThread.start();

        serverThread.join(5000);
        clientThread.join(5000);
    }

    public static void test(ObjectProviderSocket socket, Logger logger) throws IOException {
        socket.createListener().start();

        logger.info("{}\n{}",
                Arrays.toString(((Packet.PObjectList) socket.sendAndReceive(new Packet.PObjectList(Packet.nextId()))).objects),
                Arrays.toString(((Packet.PMethodList) socket.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods)
        );

        RemoteMethod toString = null;
        for (var method : ((Packet.PMethodList) socket.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods) {
            logger.info("[NAME]{}\n[ARGS]:{}\n[RETURN]:{}", method.name, Arrays.toString(method.args), method.ret);
            if (method.name.equals("toString"))
                toString = method;
        }

        logger.info("{}", ((Packet.PMethodCall) socket.sendAndReceive(new Packet.PMethodCall(Packet.nextId(), toString))).result);
    }

    @FunctionalInterface
    public interface ServerSocketSupplier {
        Server supply(int port) throws IOException;
    }

    @FunctionalInterface
    public interface ClientSocketSupplier {
        Client supply(String host, int port) throws IOException;
    }
}
