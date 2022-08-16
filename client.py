import os
import time
import array
import socket
import struct
import ctypes
import termios
import asyncio
import builtins
import threading
import collections
from enum import Enum

# Java Like Socket
class JLSocket(socket.socket):
    def __init__(self, host, port):
        super().__init__()
        self.connect((host, port))

    def sendString0(self, text: str):
        b = str.encode(text)
        self.sendNum(4, b.__len__())
        self.send(b)
    def sendDouble0(self, num: float):
        self.send(struct.pack('d', num))
    def sendLong0(self, num: int):
        self.sendNum(8, num)
    def sendInt0(self, num: int):
        self.sendNum(4, num)
    def sendShort0(self, num: int):
        self.sendNum(2, num)
    def sendByte0(self, num: int):
        self.sendNum(1, num)
    def sendNum(self, size, num: int):
        self.send(num.to_bytes(size, 'big'))

    def readString0(self) -> str:
        return self.recv(self.readInt0()).decode()
    def readDouble0(self) -> float:
        return struct.unpack('d', self.readN(8))
    def readLong0(self) -> int:
        return self.readNum(8)
    def readInt0(self) -> int:
        return self.readNum(4)
    def readShort0(self) -> int:
        return self.readNum(2)
    def readByte0(self) -> int:
        return self.readNum(1)
    def readNum(self, size) -> int:
        return int.from_bytes(self.recv(size), 'big')

class ObjectProviderSocket(JLSocket):
    def __init__(self, host, port):
        super().__init__(host, port)
        self.buffer = []
        self.metabuffer = []
        self.listener = None
        self.lock = threading.Lock()

    def createListener(self):
        if self.listener != None:
            return self.listener
        def listener():
            try:
                while(True):
                    self.listen()
            except Exception as e:
                print(e)
        self.listener = threading.Thread(target=listener)
        return self.listener

    def listen(self):
        packet = self.readPacket()
        if packet.request:
            if packet.type == Packet.Type.HELLO:
                self.sendPacket(Packet(packet.id, Packet.Type.HELLO, False))
            elif packet.type == Packet.Type.CLOSE:
                self.close()
            elif packet.type == Packet.Type.OBJECT_LIST:
                self.sendPacket(PObjectList.create1(packet.id, self.objectPool))
            elif packet.type == Packet.Type.METHOD_LIST:
                self.sendPacket(PMethodList(packet.id, packet.objectId, False))
        else:
            self.buffer.append(packet)

    def sendAndReceive(self, packet):
        id = self.sendPacket(packet)
        while(self.checkBuffer(id)):pass
        for e in self.buffer:
            if e.id == id:
                return e
        raise Exception("WTF XD?!")

    def checkBuffer(self, id):
        for e in self.buffer:
            if e.id == id:
                return False
        return True

    def sendObject(self, arr):
        t = ARType.of(arr)
        size = -1 if t.dim == 0 else arr.__len__()
        self.sendInt(size)
        if size > -1:
            for i in range(0, size):
                self.sendObject(arr[i])
        else:
            self.sendEnum(t.type)
            if (isinstance(arr, bool)):
                arr = 1 if arr else 0
            self.writeWithType(arr, t.type)
    def writeWithType(self, obj, t):
        if t == PType.BYTE:
            self.sendByte(obj)
        elif t == PType.SHORT:
            self.sendShort(obj)
        elif t == PType.INT:
            self.sendInt(obj)
        elif t == PType.LONG:
            self.sendLong(obj)
        elif t == PType.DOUBLE:
            self.sendDouble(obj)
        elif t == PType.STRING:
            self.sendString(obj)
        elif t == PType.ENUM:
            self.sendEnum(obj)
        elif t == PType.PACKET:
            self.sendPacket(obj)
        elif t == PType.OBJECT:
            self.objectPool.append(obj)
            self.sendInt0(self.objectPool.index(obj))
    def sendPacket(self, packet) -> int:
        self.lock.acquire()
        self.sendByte0(7)
        packet.write(self)
        self.lock.release()
        return packet.id
    def sendARType(self, type):
        self.sendByte0(10)
        self.sendInt0(type.dim)
        self.sendEnum(type.type)
    def sendEnum(self, enum):
        self.sendByte0(6)
        self.sendString0(enum.name)
    def sendString(self, text: str):
        self.sendByte0(0)
        self.sendString0(text)
    def sendDouble(self, num: float):
        self.sendByte0(1)
        self.sendDouble0(num)
    def sendLong(self, num: int):
        self.sendByte0(2)
        self.sendLong0(num)
    def sendInt(self, num: int):
        self.sendByte0(3)
        self.sendInt0(num)
    def sendShort(self, num: int):
        self.sendByte0(4)
        self.sendShort0(num)
    def sendByte(self, num: int):
        self.sendByte0(5)
        self.sendByte0(num)
    def sendBoolean(self, state: bool):
        if state:
            self.sendByte(1)
        else:
            self.sendByte(0)

    def readObject(self):
        size = self.readInt0()
        if (size > -1):
            arr = []
            for i in range(0, size):
                arr[i] = self.readObject()
            return arr
        else:
            t = self.readEnum(PType)
            if (t == PType.BYTE):
                return self.readByte()
            if (t == PType.SHORT):
                return self.readShort()
            if (t == PType.INT):
                return self.readInt()
            if (t == PType.LONG):
                return self.readLong()
            if (t == PType.DOUBLE):
                return self.readDouble()
            if (t == PType.STRING):
                return self.readString()
            if (t == PType.ENUM):
                self.checkValue(6)
                return self.readString0()
            if (t == PType.PACKET):
                return self.readPacket()
            if (t == Ptype.NULL):
                return None
            if (t == PType.OBJECT):
                return RemoteObjectImpl(self.readInt0()) # TODO:
            raise Exception("Invalid type!")
    def readPacket(self):
        self.checkValue(7)
        return Packet.read(self)
    def readARType(self):
        self.checkValue(10)
        return ARType(self.readInt0(), self.readEnum(PType))
    def readEnum(self, enum):
        self.checkValue(6)
        return enum[self.readString0()]
    def readString(self) -> str:
        self.checkValue(0)
        return self.readString0()
    def readDouble(self) -> float:
        self.checkValue(1)
        return self.readDouble0()
    def readLong(self) -> int:
        self.checkValue(2)
        return self.readLong0()
    def readInt(self) -> int:
        self.checkValue(3)
        return self.readInt0()
    def readShort(self) -> int:
        self.checkValue(4)
        return self.readShort0()
    def readByte(self) -> int:
        self.checkValue(5)
        return self.readByte0()
    def readBoolean(self) -> bool:
        return self.readByte() == 1
    def checkValue(self, needed: int):
        if self.readByte0() != needed:
            raise Exception("Invalid value!")

class Client(ObjectProviderSocket):
    def __init__(self, host, port):
        super().__init__(host, port)
        self.objectPool = []
    
    def close(self):
        self.sendPacket(Packet(Packet.nextId(), Packet.Type.CLOSE, True))
        super().close()

class Packet:
    LAST_ID = 0

    def nextId() -> int:
        LAST_ID = Packet.LAST_ID
        Packet.LAST_ID += 1
        return LAST_ID

    def __init__(self, id, type, request):
        self.id = id
        self.type = type
        self.request = request
    
    def read(sock):
        id = sock.readInt()
        t = sock.readEnum(Packet.Type)
        request = sock.readBoolean()
        if t == Packet.Type.HELLO or t == Packet.Type.CLOSE:
            return Packet(id, t, request)
        if t == Packet.Type.OBJECT_LIST:
            if request:
                return PObjectList.create0(id)
            else:
                return PObjectList.create2(id, sock)
        if t == Packet.Type.METHOD_LIST:
            if request:
                return PMethodList(id, sock.readInt(), True)
            else:
                return PMethodList.create0(id, sock)
        if t == Packet.Type.METHOD_CALL:
            return PMethodCall.create1(id, sock, request)
        raise Exception("Invalid packet №{}!".format(id))
    
    def preWrite(self, sock):
        pass

    def write(self, sock):
        sock.sendInt(self.id)
        sock.sendEnum(self.type)
        sock.sendBoolean(self.request)

    class Type(Enum):
        HELLO = "HELLO"
        CLOSE = "CLOSE"
        #
        OBJECT_LIST = "OBJECT_LIST"
        METHOD_LIST = "METHOD_LIST"
        #
        METHOD_CALL = "METHOD_CALL"

class PMethodList(Packet):
    def __init__(self, pid: int, oid: int, request: bool):
        super().__init__(pid, Packet.Type.METHOD_LIST, request)
        self.objectId = oid
        self.methods = None
    
    def create0(pid: int, sock):
        instance = PMethodList(pid, sock.readInt(), False)
        instance.methods = []
        for i in range(0, sock.readInt()):
            name = sock.readString()
            args = []
            for j in range(0, sock.readInt()):
                args.append(sock.readARType())
            instance.methods.append(RemoteMethod(name, args, sock.readARType(), instance.objectId))
        return instance
    
    def write(self, sock):
        super().write(sock)
        sock.sendInt(self.objectId)
        if self.request:
            return
        clazz = sock.objectPool[self.objectId].__class__
        attributes = [getattr(clazz, attr) for attr in dir(clazz)]
        methods = [attr for attr in attributes if attr.__class__.__name__ == 'builtin_function_or_method' or attr.__class__.__name__ == 'function']
        sock.sendInt(methods.__len__())
        for method in methods:
            sock.sendString(method.__name__)
            sock.sendInt(1)
            sock.sendARType(ARType(1, PType.OBJECT))
            sock.sendARType(ARType(0, PType.OBJECT))

class PObjectList(Packet):
    def __init__(self, id: int, request: bool):
        super().__init__(id, Packet.Type.OBJECT_LIST, request)
        self.objects = []

    def create0(id: int):
        return PObjectList(id, True)
    def create1(id: int, objects: list):
        instance = PObjectList(id, False)
        instance.objects = list(map(lambda o: builtins.id(o), objects))
        return instance
    def create2(id: int, sock: ObjectProviderSocket):
        instance = PObjectList(id, False)
        i = sock.readInt()
        for _ in range(0, i):
            instance.objects.append(sock.readLong())
        return instance
        
    def write(self, sock):
        super().write(sock)
        if not self.request:
            sock.sendInt(self.objects.__len__())
            for e in self.objects:
                sock.sendLong(e)

class PMethodCall(Packet):
    def __init__(self, id: int, method, args, result, request: bool):
        super().__init__(id, Packet.Type.METHOD_CALL, request)
        self.method = method
        self.args = args
        self.result = result

    def create0(id: int, method, args):
        return PMethodCall(id, method, args, None, True)
    
    def create1(id: int, sock: ObjectProviderSocket, request: bool):
        instance = PMethodCall(id, None, None, None, request)
        if (request):
            obj = sock.readInt()
            name = sock.readString()
            l = sock.readInt()
            args = []
            for i in range(0, l):
                args[i] = sock.readARType()
            ret = sock.readARType()
            instance.args = sock.readObject()
            instance.method = RemoteMethod(name, args, ret, obj) # TODO:
            instance.result = None
        else:
            packet = [e for e in sock.metabuffer if e.id == instance.id][0]
            instance.method = packet.method
            selinstancef.args = packet.args
            instance.result = sock.readObject()
        return instance

    def create2(self, request):
        return PMethodCall(id, request.method, request.args, None, False)

    def preWrite(self, sock):
        self.result = sock.invokeMethod(self.method, self.args)

    def write(self, sock):
        super().write(sock)
        if self.request:
            sock.writeInt(self.method.obj)
            sock.writeString(self.method.name)
            sock.writeInt(self.method.args.__len__())
            for arg in self.method.args:
                sock.writeARType(arg)
            sock.writeARType(self.method.ret)
            sock.writeObject(self.args)
            sock.metabuffer.append(self)
        else:
            sock.writeObject(self.result)

class RemoteMethod:
    def __init__(self, name: str, args: list, ret, obj: int):
        self.name = name
        self.args = args
        self.ret = ret
        self.obj = obj

class ARType:
    def __init__(self, dim: int, type):
        self.dim = dim
        self.type = type
    
    def of(obj): # TODO:
        if obj == None:
            return ARType(0, PType.NULL)
        return ARType(0, PType.of(obj))

    def __repr__(self):
        return f"<{self.type}[{self.dim}]>"

class PType(Enum):
    STRING = 0
    DOUBLE = 1
    LONG = 2
    INT = 3
    SHORT = 4
    BYTE = 5
    ENUM = 6
    PACKET = 7
    OBJECT = 8
    NULL = 9

    def of(obj):
        if isinstance(obj, str):
            return PType.STRING
        if isinstance(obj, float):
            return PType.DOUBLE
        if isinstance(obj, int):
            return PType.INT
        if isinstance(obj, Enum):
            return PType.ENUM
        if isinstance(obj, Packet):
            return PType.PACKET
        if isinstance(obj, None):
            return PType.NULL
        return PType.OBJECT

# Test section

import logging

class TestClassA:
    def toString(self):
        return self.__str__()

class TestClassB:
    def __init__(self, i: int):
        self.i = i

    def add(self, j):
        if isinstance(j, int):
            return TestClass(self.i + j)
        if isinstance(j, TestClass):
            return TestClass(self.i + j.i)

if __name__ == '__main__':
    logging.basicConfig(filename="PythonTest.log",level=logging.INFO)

    client = Client('localhost', 2014)
    client.objectPool.append(TestClassA())
    client.objectPool.append(TestClassB(21))

    client.createListener().start()

    logging.info("%s\n%s\n",
        client.sendAndReceive(PObjectList.create0(Packet.nextId())).objects,
        client.sendAndReceive(PMethodList(Packet.nextId(), 0, True)).methods
    )

    for method in client.sendAndReceive(PMethodList(Packet.nextId(), 0, True)).methods:
        logging.info("[NAME]%s\n[ARGS]%s\n[RETURN]%s\n", method.name, method.args, method.ret)
    input("Press Enter to continue...")
    client.close()