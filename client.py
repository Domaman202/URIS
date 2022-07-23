import os
import fcntl
import array
import socket
import struct
import ctypes
import termios
import asyncio
import threading
import collections
from enum import Enum

class Client:
    def __init__(self, host, port):
        self.pool = []
        self.sock = socket.socket()
        self.sock.connect((host, port))
        assert self.readPacket().type == PacketType.HELLO
        self.sendPacket(Packet(PacketType.HELLO))
        print('Connection success!')
    
    def sendObject(self, obj):
        if type(obj) is Packet:
            self.sendPacket(obj)
        elif type(obj) is Enum:
            self.sendEnum(obj)
        else:
            t = Type.of(obj)
            self.sendNum(1, t.value)
            self.sendWithType(t, obj)
    def sendWithType(self, t, obj):
        if t == Type.STRING:
            self.sendStringI(obj)
        elif t == Type.OBJECT:
            pass # TODO: объекты >:)
        elif t != Type.NULL:
            if t == Type.DOUBLE:
                self.sendDoubleI(obj)
            else:
                size = 0
                if t == Type.BYTE:
                    size = 1
                elif t == Type.SHORT:
                    size = 2
                elif t == Type.INT:
                    size = 4
                elif t == Type.LONG:
                    size = 8
                self.sendNum(size, obj)
    def sendPacket(self, packet):
        self.sendNum(1, 7)
        packet.write(self)
    def sendEnum(self, enum):
        self.sendNum(1, 6)
        self.sendString(enum.name)
    def sendString(self, text):
        self.sendNum(1, 0)
        self.sendStringI(text)
    def sendDouble(self, num):
        self.sendNum(1, 1)
        self.sendDoubleI(num)
    def sendLong(self, num):
        self.sendNum(1, 2)
        self.sendNum(8, num)
    def sendInt(self, num):
        self.sendNum(1, 3)
        self.sendNum(4, num)
    def sendShort(self, num):
        self.sendNum(1, 4)
        self.sendNum(2, num)
    def sendByte(self, num):
        self.sendNum(1, 5)
        self.sendNum(1, num)
    def sendStringI(self, text):
        b = str.encode(text)
        self.sendNum(4, b.__len__())
        self.sock.send(b)
    def sendDoubleI(self, num):
        self.sock.send(struct.pack('d', num))
    def sendNum(self, size, num):
        self.sock.send(num.to_bytes(size, 'big'))

    def readObject(self):
        t = self.readNum(1)
        if t == 0:
            return self.readStringI()
        if t == 1:
            return self.readDoubleI()
        if t == 2:
            return self.readNum(8)
        if t == 3:
            return self.readNum(4)
        if t == 4:
            return self.readNum(2)
        if t == 5:
            return self.readNum(1)
        if t == 6:
            return self.readString()
        if t == 7:
            return self.readPacketI()
        if t == 8:
            return None # TODO: объекты >:)
        if t == 9:
            return None
    def readPacket(self):
        assert self.readNum(1) == 7
        return self.readPacketI()
    def readEnum(self, enum):
        assert self.readNum(1) == 6
        return enum[self.readString()]
    def readString(self):
        assert self.readNum(1) == 0
        return self.readStringI()
    def readDouble(self):
        assert self.readNum(1) == 1
        return self.readDoubleI()
    def readLong(self):
        assert self.readNum(1) == 2
        return self.readNum(8)
    def readInt(self):
        assert self.readNum(1) == 3
        return self.readNum(4)
    def readShort(self):
        assert self.readNum(1) == 4
        return self.readNum(2)
    def readByte(self):
        assert self.readNum(1) == 5
        return self.readNum(1)
    def readPacketI(self):
        return Packet(self.readEnum(PacketType))
    def readStringI(self):
        return self.sock.recv(self.readNum(4)).decode()
    def readDoubleI(self):
        return struct.unpack('d', self.readNum(8))
    def readNum(self, size):
        return int.from_bytes(self.sock.recv(size), 'big')

    def listener(self):
        self.sync = False
        self.lresult = collections.deque()
        while(True):
            b = self.readNum(1)
            if b == 7:
                result = self.listen0(self.readPacketI())
                if self.sync:
                    self.lresult.append(result)
            elif b == 0 or result.type == PacketType.CLOSE:
                break
            else:
                print('WTF -> ' + b)
                break

    def listen(self):
        return self.listen0(self.readPacket())
    def listen0(self, packet):
        t = packet.type
        if t == PacketType.TEST_PACKET:
            print('TEST PACKET RECIVED!')
        elif t == PacketType.OBJECT_LIST_REQUEST:
            self.sendPacket(PacketObjectList(self.pool.__len__()))
        elif t == PacketType.OBJECT_LIST:
            return PacketObjectList(self.readNum(4))
        return packet
    
    def sendPacketObjectList(self):
        self.sendPacket(Packet(PacketType.OBJECT_LIST_REQUEST))
        return self.listen()
    
    # class RemoteObject:
    #     def __init__(self, outer, id):
    #         self.outer = outer
    #         self.id = id
    #         pass

class Packet(object):
    def __init__(self, type):
        self.type = type

    def write(self, client):
        client.sendEnum(self.type)

class PacketObjectList(Packet):
    def __init__(self, count):
        super().__init__(PacketType.OBJECT_LIST)
        self.count = count

    def write(self, client):
        super().write(client)
        client.sendNum(4, self.count)

class PacketType(Enum):
    TEST_PACKET = "TEST_PACKET"
    HELLO = "HELLO"
    OBJECT_LIST_REQUEST = "OBJECT_LIST_REQUEST"
    OBJECT_LIST = "OBJECT_LIST"
    METHOD_LIST_REQUEST = "METHOD_LIST_REQUEST"
    METHOD_LIST = "METHOD_LIST"
    INVOKE = "INVOKE"
    RETURN = "RETURN"
    CLOSE = "CLOSE"

class Type(Enum):
    BYTE = 5
    SHORT = 4
    INT = 3
    LONG = 2
    DOUBLE = 1
    STRING = 0
    OBJECT = 8
    NULL = 9

    def of(obj):
        if obj is None:
            return Type.NULL
        t = type(obj)
        if t is int:
            return Type.INT
        if t is float:
            return Type.DOUBLE
        if t is str:
            return Type.STRING
        return Type.OBJECT

if __name__ == '__main__':
    client = Client('localhost', 2022)
    thr = threading.Thread(target=client.listener);
    thr.start()
    client.sync = True
    input("Press Enter to continue...\n")
    print(client.lresult.popleft().type)
    print(client.lresult.popleft().type)
    client.sendPacket(Packet(PacketType.CLOSE))