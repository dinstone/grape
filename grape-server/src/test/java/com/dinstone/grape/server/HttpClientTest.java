/*
 * Copyright (C) 2016~2019 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.grape.server;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientTest {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientTest.class);

    public static void main(String[] args) throws UnknownHostException, Exception {
        List<Socket> sl = new ArrayList<Socket>();
        for (int i = 0; i < 20000; i++) {
            Socket s = new Socket();
//            s.setSoTimeout(3000);
            long n = System.currentTimeMillis();
            try {
                s.connect(new InetSocketAddress("localhost", 8081), 3000);
            } finally {
                long l = System.currentTimeMillis() - n;
                LOG.info("connection index[{}]-{}ms, {}", i, l, s.getLocalSocketAddress());
            }
//            OutputStream out = s.getOutputStream();
//            String request = "GET /pay-api HTTP/1.1\r\n" + "Host: localhost:8080\r\n"
//                    + "Connection: keep-alive\r\n\r\n";
//            out.write(request.getBytes());
//            
//            InputStream in = s.getInputStream();
//            in.read();

            // s.close();

            sl.add(s);
        }

        System.in.read();

        for (Socket socket : sl) {
            socket.close();
        }

        System.in.read();
    }

}
