package com.dinstone.grape.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientTest {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientTest.class);

    public static void main(String[] args) throws UnknownHostException, Exception {
        for (int i = 0; i < 20; i++) {
            Socket s = new Socket("localhost", 8081);
            LOG.info("connection = " + s.getLocalSocketAddress());
            OutputStream out = s.getOutputStream();
            String request = "GET /pay-api HTTP/1.1\r\n" + "Host: localhost:8080\r\n"
                    + "Connection: keep-alive\r\n\r\n";
            out.write(request.getBytes());
            
            InputStream in = s.getInputStream();
            in.read();
            
            s.close();
        }

        System.in.read();
    }

}
