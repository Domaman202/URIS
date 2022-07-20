package ru.uris;

public enum PacketType {
        HELLO_PACKET,
        OBJECT_LIST_REQUEST,
        OBJECT_LIST,
        METHOD_LIST_REQUEST,
        METHOD_LIST,
        INVOKE,
        RETURN,
        CLOSE
}