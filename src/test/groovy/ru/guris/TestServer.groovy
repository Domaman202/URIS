package ru.guris

class TestServer {
    static void main(String[] args) {
        try (var server = new Server(2014)) {
            server.objectPool.add(new Object());
            server.objectPool.add(new TestClass(12));

            server.objectPool.add(TestServer.class.getClassLoader());
            var connection = server.accept();
            connection.createListener().start();

            System.out.println(Arrays.toString(((Packet.PObjectList) connection.sendAndReceive(new Packet.PObjectList(Packet.nextId()))).objects));
            System.out.println(Arrays.toString(((Packet.PMethodList) connection.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods));
            System.out.println();
            for (method in ((Packet.PMethodList) connection.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods)
                System.out.println("[NAME]${method.name}\n[ARGS]:${Arrays.toString(method.args)}\n[RETURN]:${method.ret}\n");
        }
    }
}
