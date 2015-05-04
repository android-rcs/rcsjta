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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sonymobile.rcs.imap.mock.MockIoService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class DefaultImapServiceInitTest {

    MockIoService serverIO;
    DefaultImapService service;

    @Before
    public void init() throws IOException, ImapException {
        serverIO = new MockIoService();
        service = new DefaultImapService(serverIO);
        service.setAuthenticationDetails("user", "pass", null, null, false);
    }

    @After
    public void close() throws IOException {
        service.close();
    }

    @Test
    public void testConnect() throws IOException {
        assertFalse(service.isAvailable());
        service.getIoService().connect();
        assertTrue(service.isAvailable());
        service.getIoService().close();
        assertFalse(service.isAvailable());
    }

    @Test
    public void testLoginSuccess() throws IOException, ImapException {
        service.login();

        List<String> capabilities = service.getCapabilities();
        assertTrue(capabilities.contains("IMAP4rev1"));

        service.logout();

        service.close();
    }

    @Test
    public void testLoginFailed() throws IOException, ImapException {
        try {
            serverIO.setNextResponseOk(false);
            service.login();
            fail("Login should fail");
        } catch (ImapException e) {
            // continue
        }
        service.close();
    }

    @Test
    public void testPersonalNamespaceWithNamespaceCapability() throws IOException, ImapException {
        serverIO.setFolderPreffix("--preffix--");
        serverIO.setFolderSeparator(".");

        serverIO.addCapability("NAMESPACE");

        assertNull(service.getPersonalNamespace());

        service.login();

        assertNotNull(service.getPersonalNamespace());

        assertEquals(".", service.getPersonalNamespace().getDelimiter());
        assertEquals("--preffix--", service.getPersonalNamespace().getPreffix());
    }

}
