package ru.uris.test.java;

import ru.uris.Client;
import ru.uris.Server;
import ru.uris.test.main.MainTest;

import java.io.IOException;

public class TestImpl {
    public static void main(String[] args) throws InterruptedException, IOException {
        MainTest.main(Server::new, Client::new, "JavaTest");
    }
}
