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

package com.sonymobile.rcs.imap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sonymobile.rcs.imap.mock.MockRemoteServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class SocketIoServiceTest {

    MockRemoteServer server = new MockRemoteServer();

    SocketIoService io;

    @Before
    public void startServer() throws IOException {
        server.start();
    }

    @After
    public void stopServer() {
        server.stop();
    }

    @Test
    public void testConnectReadAndWrite() throws Exception {
        io = new SocketIoService(server.getUri());
        io.connect();
        assertTrue(io.isConnected());

        String welcome = io.readLine();
        assertEquals("Welcome!", welcome);

        io.writeln("HereIsMyCommand");
        assertEquals("HereIsMyReply", io.readLine());

        String data = "Base64String";
        io.writeln("Take " + data.length());

        io.write(data);

        String data2 = io.read(data.length());
        assertEquals(data, data2);

        /*
         * assertEquals(data, server.read(data.length())); String data2 = "AnotherDataString";
         * server.write(data2); String res = io.read(data2.length()); assertEquals(data2, res);
         */
    }

    // TODO SSL HANDSHAKE

}
