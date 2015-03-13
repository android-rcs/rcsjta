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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sonymobile.rcs.imap.mock.MockIoService;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultImapServiceTest {

    MockIoService mock;
    DefaultImapService service;

    @BeforeClass
    public static void init() {
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.FINEST);
        Logger.getLogger("").addHandler(ch);
        Logger.getLogger("com.sonymobile").setLevel(Level.ALL);
    }

    @Before
    public void setUpConnectAndLogin() throws IOException, ImapException {
        mock = new MockIoService();
        mock.addCapability("NAMESPACE");

        service = new DefaultImapService(mock);
        service.login();

        service.resetLastTimeRequested();
    }

    @After
    public void tearDown() throws IOException {
        mock.reset();
        service.close();
    }

    private void assertLastTimeRequestedIsUpdated() {
        long now = System.currentTimeMillis();
        assertTrue((now - service.getLastTimeRequested()) < 20);
    }

    @Test
    public void testGetFolderNames() throws IOException, ImapException {
        // mock.debug();
        List<ImapFolder> folderNames = service.getFolders("MyRoot", true);
        folderNames = service.getFolders("MyRoot", false);
        assertLastTimeRequestedIsUpdated();
    }

    @Test
    public void testCreateDelete() throws IOException, ImapException {
        boolean b = service.create("MyNewFolder");
        assertTrue(b);
        b = service.delete("MyNewFolder");
        assertTrue(b);
    }

    @Test
    public void testRename() throws IOException, ImapException {
        service.rename("old", "new");

        assertLastTimeRequestedIsUpdated();
    }

    @Test
    public void testUnselect() throws IOException, ImapException {
        try {
            service.unselect();
            fail("cabability unchecked : unselect");
        } catch (CapabilityNotSupportedException e) {
            // ok
        }

        mock.addCapability("UNSELECT");
        service.clearCapabilities();
        service.getCapabilities();
        service.unselect();

        assertLastTimeRequestedIsUpdated();
    }

    @Test
    public void testSelectFolder() throws IOException, ImapException {
        String[] response = {
                "* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen)",
                "* 12 EXISTS",
                "* 2 RECENT",
                "* OK [UIDVALIDITY 1744381950] UIDs valid",
                "* OK [PERMANENTFLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen \\*)] Limited",
                "* OK [HIGHESTMODSEQ 1] Highest", "* OK [UIDNEXT 34] Predicted next UID"
        };
        mock.setNextResponseForCommand("select", response);
        ImapFolderStatus status = service.select("aFolder");
        assertNotNull(status);

        assertEquals(12, status.getExists());
        assertEquals(2, status.getRecent());
        assertEquals(34, status.getNextUid());
        assertEquals(1744381950, status.getUidValidity());

        assertLastTimeRequestedIsUpdated();
    }

    @Test
    public void testGetFolderStatus() throws IOException {
        // mock.debug();
        mock.setNextResponseForCommand("STATUS",
                "\"folder1\" (MESSAGES 7 RECENT 2 UIDNEXT 18 UIDVALIDITY 229669678 UNSEEN 3)");
        ImapFolderStatus status = service.getFolderStatus("folder1");
        assertNotNull(status);

        assertEquals(7, status.getExists());
        assertEquals(2, status.getRecent());
        assertEquals(18, status.getNextUid());
        assertEquals(3, status.getUnseen());
        assertEquals(229669678, status.getUidValidity());

        assertLastTimeRequestedIsUpdated();
    }

    @Test
    public void testExpunge() throws IOException, ImapException {
        service.expunge();

        assertLastTimeRequestedIsUpdated();
    }

    @Test
    public void testNoop() throws Exception {
        service.noop();

        assertLastTimeRequestedIsUpdated();
    }

    @Test
    public void testExamine() throws IOException, ImapException {
        String[] response = {
                "* FLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen)",
                "* 12 EXISTS",
                "* 2 RECENT",
                "* OK [UIDVALIDITY 1744381950] UIDs valid",
                "* OK [PERMANENTFLAGS (\\Answered \\Deleted \\Draft \\Flagged \\Seen \\*)] Limited",
                "* OK [HIGHESTMODSEQ 1] Highest", "* OK [UIDNEXT 34] Predicted next UID"
        };
        mock.setNextResponseForCommand("EXAMINE", response);
        ImapFolderStatus status = service.examine("folder1");
        assertNotNull(status);

        assertEquals(12, status.getExists());
        assertEquals(2, status.getRecent());
        assertEquals(34, status.getNextUid());
        assertEquals(1744381950, status.getUidValidity());
    }

    @Test
    public void testSearchMessages() throws IOException, ImapException {
        // mock.debug();
        mock.setNextResponseForCommand("uid search", "1 2 3 4 5 6 44441111");
        int[] ids = service.searchMessages(new Search().all());
        assertEquals(7, ids.length);
        assertEquals(1, ids[0]);
        assertEquals(2, ids[1]);
        assertEquals(3, ids[2]);
        assertEquals(4, ids[3]);
        assertEquals(5, ids[4]);
        assertEquals(6, ids[5]);
        assertEquals(44441111, ids[6]);
    }

    @Test
    public void testAppend() {
        // TODO NIS
        // fail("Not yet implemented");
    }

    @Test
    public void testFetchMessage() throws IOException, ImapException {
        /*
         * mock.debug(); String [] rsp = { "* 1 FETCH (BODY[] {110}", "Message-ID:1",
         * "Conversation-Id:A12345", "From:Joe <joe@sony.com>",
         * "Hello this is a first text message from Joe", ")" + }
         * mock.setNextResponseForCommand("fetch", responses); IMAPMessage msg =
         * service.fetchMessage(1);
         */
    }

    // FIXME ASAP
    @Test
    public void testFetchMessageById() throws IOException, ImapException {
        mock.debug();

        String[] responses = {
                "* 1 FETCH (UID 2 BODY[] {647}",
                "Return-Path: <kamal@localhost>",
                "Delivered-To: kamal@localhost",
                "Received: from localhost (EHLO [127.0.0.1]) ([127.0.0.1])",
                "          by seldlx20876 (JAMES SMTP Server ) with ESMTP ID 194560441",
                "          for <kamal@localhost>;",
                "          Tue, 01 Jul 2014 03:57:20 +0200 (CEST)",
                "Message-ID: <53B2BE40.9020202@localhost>",
                "Date: Tue, 01 Jul 2014 15:57:20 +0200",
                "From: Kamal <kamal@localhost>",
                "User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:24.0) Gecko/20100101 Thunderbird/24.6.0",
                "MIME-Version: 1.0", "To: kamal@localhost", "Subject: second one",
                "Content-Type: text/plain; charset=ISO-8859-1; format=flowed",
                "Content-Transfer-Encoding: 7bit", "", "second test", ")"
        };

        mock.setNextResponseForCommand("uid fetch", responses);
        ImapMessage msg = service.fetchMessageById(1);
        Part part = msg.getBody();
        assertNotNull(part);

        assertEquals("second test", part.getContent().trim());
        assertEquals("text/plain", part.getContentType().trim().substring(0, 10));
        assertEquals("kamal@localhost", part.getHeader("To"));
    }

    @Test
    public void testReadMessageMetadata() throws IOException, ImapException {
        // mock.debug();

        mock.setNextResponseForCommand(
                "uid fetch",
                "* 1 FETCH (FLAGS (\\Seen) INTERNALDATE \"11-Aug-2014 11:42:58 +0000\" RFC822.SIZE 110 ENVELOPE (NIL NIL ((\"Joe\" NIL \"joe\" \"sony.com\")) ((\"Joe\" NIL \"joe\" \"sony.com\")) ((\"Joe\" NIL \"joe\" \"sony.com\")) NIL NIL NIL NIL \"1\"))");
        ImapMessageMetadata m = service.fetchMessageMetadataById(1);
        assertNotNull(m);
        assertEquals(1, m.getFlags().size());
        assertTrue(m.getFlags().contains(Flag.Seen));

    }

    @Test
    public void testUpdateFlags() throws IOException, ImapException {
        service.addFlags(1234, Flag.Deleted);

        service.removeFlags(1234, Flag.Deleted);

        service.addFlags("1:*", Flag.Deleted);

        service.removeFlags("1:2", Flag.Deleted);
    }

    @Test
    public void testCopy() throws IOException, ImapException {
        // mock.debug();
        service.copy("1:*", "destinationFolder");
    }

    @Test
    public void testIdling() {
        // TODO NIS
        // fail("Not yet implemented");
    }

    @Test
    public void testFolderMetadata() throws IOException, ImapException {

        mock.addCapability("METADATA");

        service.clearCapabilities();
        service.getCapabilities();

        mock.setNextResponseForCommand("GETMETADATA",
                "* METADATA \"\" (/shared/comment \"lala comment\")");

        String someString = service.getFolderMetadata("somefolder");
        assertEquals("lala comment", someString);
        service.setFolderMetadata("somefolder", "someOtherString=abcd");

    }

    @Test
    public void testFolderMetadata_capabilitycheck() throws IOException, ImapException {

        try {
            mock.setNextResponseForCommand("GETMETADATA",
                    "* METADATA \"\" (/shared/comment \"Shared comment\")");
            String someString = service.getFolderMetadata("somefolder");
            assertEquals("Shared comment", someString);
            fail("no capability check for metadata");
        } catch (CapabilityNotSupportedException e) {
            // ok
        }

        try {
            service.setFolderMetadata("somefolder", "someOtherString=abcd");
            fail("no capability check for metadata");
        } catch (CapabilityNotSupportedException e) {
            // ok
        }

    }

}
