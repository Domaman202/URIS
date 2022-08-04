package ru.guris

class TestClient {
    static void main(String[] args) {
        try (var client = new Client("localhost", 2014)) {
            client.objectPool.add(new Object());

            client.createListener().start();

            System.out.println(Arrays.toString(((Packet.PObjectList) client.sendAndReceive(new Packet.PObjectList(Packet.nextId()))).objects));
            System.out.println(Arrays.toString(((Packet.PMethodList) client.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods));
            System.out.println();
            for (method in ((Packet.PMethodList) client.sendAndReceive(new Packet.PMethodList(Packet.nextId(), 0, true))).methods)
                System.out.println("[NAME]${method.name}\n[ARGS]:${Arrays.toString(method.args)}\n[RETURN]${method.ret}\n");
        }
    }
}
