package ru.uris.test.groovy;

import ru.uris.gadapter.GroovyClient;
import ru.uris.gadapter.GroovyServer;

import java.io.IOException;

public class TestImpl {
    public static void main(String[] args) throws InterruptedException, IOException {
        ru.uris.test.main.MainTest.main(GroovyServer::new, GroovyClient::new, "GroovyTest");
    }
}
