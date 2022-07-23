import threading
from socket import socket
from collections import deque

class SmartSocket(socket):
    def __init__(self, host, port):
        super().__init__()
        self.connect((host, port))
        self.buffer = deque()
        self.wait = False
        self.wthread = threading.current_thread()
        self.pb = None

    def readN(self, size):
        if self.wthread != threading.current_thread():
            while(self.wait):pass
        b = bytearray()
        for i in range(0, size):
            b.append(self._read())
        return b

    def read(self):
        if self.wthread != threading.current_thread():
            while(self.wait):pass
        return self._read()
    
    def _read(self):
        if self.buffer.__len__() == 0:
            byte = super().recv(1)
            if byte.__len__() == 0:
                return 0
            self.buffer.append(byte[0])
        self.pb = self.buffer.popleft()
        return self.pb
    
    def uread(self):
        if self.pb != None:
            self.buffer.appendleft(self.pb)
            self.pb = None

# #DEBUG ONLY:
# if __name__ == "__main__":
#     sock = SmartSocket("localhost", 2022)
#     sock.allow = True
#     print(sock.readN(1))
#     print(sock.readN(1))
#     sock.allow = False
#     print(sock.read())