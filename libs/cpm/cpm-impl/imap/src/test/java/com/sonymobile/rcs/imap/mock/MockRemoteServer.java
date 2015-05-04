/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.imap.mock;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocketFactory;

public class MockRemoteServer implements Runnable {

    public static final int PORT = 8888;

    private boolean startAsSSL;

    private ServerSocket serverSocket = null;

    private Socket clientSocket = null;

    private PrintWriter out = null;

    private BufferedReader in;

    private boolean clientCreated;

    private String nextLine = null;

    private boolean stopped = true;

    public void setStartAsSSL(boolean startAsSSL) {
        this.startAsSSL = startAsSSL;
    }

    public boolean isClientCreated() {
        return clientCreated;
    }

    public String getUri() {
        String protocol = startAsSSL ? "imaps" : "imap";
        return protocol + "://localhost:" + PORT;
    }

    public void start() throws IOException {
        if (startAsSSL) {
            serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(PORT);
        } else {
            serverSocket = new ServerSocket(PORT);
        }

        stopped = false;
        new Thread(MockRemoteServer.this).start();
    }

    @Override
    public void run() {
        try { // Will only accept one for this test
            clientSocket = serverSocket.accept();
            clientCreated = true;
            if (stopped)
                return;
            OutputStream outStream = clientSocket.getOutputStream();
            out = new PrintWriter(outStream, true);
            InputStream inStream = clientSocket.getInputStream();
            in = new BufferedReader(new InputStreamReader(inStream));

            out.println("Welcome!");
            String line = null;
            while ((!stopped) && ((line = in.readLine()) != null)) {
                nextLine = line;
                if (nextLine.startsWith("Take")) {
                    int size = Integer.parseInt(nextLine.substring(4).trim());
                    ByteArrayOutputStream boo = new ByteArrayOutputStream();
                    int j = 0;
                    while (true) {
                        if (inStream.available() > 0) {
                            byte[] d = new byte[inStream.available()];
                            inStream.read(d);
                            boo.write(d);
                            j += d.length;
                            if (j >= size)
                                break;
                        }
                    }
                    byte[] received = boo.toByteArray();
                    outStream.write(received);
                }
                if (nextLine.startsWith("HereIsMyCommand")) {
                    out.println("HereIsMyReply");
                }
            }

        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    public void stop() {
        try {
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stopped = true;
        }
    }
}
