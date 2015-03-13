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

package com.sonymobile.rcs.cpm.ms.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sonymobile.rcs.cpm.ms.ConversationHistoryFolder;
import com.sonymobile.rcs.cpm.ms.CpmChatMessage;
import com.sonymobile.rcs.cpm.ms.CpmMessageStore;
import com.sonymobile.rcs.cpm.ms.CpmObject;
import com.sonymobile.rcs.cpm.ms.RootFolder;
import com.sonymobile.rcs.cpm.ms.SessionHistoryFolder;
import com.sonymobile.rcs.cpm.ms.impl.mock.MockIMAPService;
import com.sonymobile.rcs.imap.ImapNamespace;
import com.sonymobile.rcs.imap.Part;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CommonMessageStoreImplTest {

    private static final String DEFAULT_FOLDER = "cpmdefault";

    MockIMAPService imapService = new MockIMAPService();

    CpmMessageStore mStore = new CommonMessageStoreImpl(DEFAULT_FOLDER, imapService);

    @Before
    public void setUp() throws Exception {
        imapService.setPersonalNamespace(new ImapNamespace("", "/"));
        imapService.create("cpmdefault/conv1");
        imapService.create("cpmdefault/conv1/sess1");
        imapService.create("cpmdefault/conv2");
        imapService.append("cpmdefault/conv1", null, new Part("text/plain", "Hello World!"));
        imapService.append("cpmdefault/conv1/sess1", null, new Part("text/plain",
                "Hello World In Session!"));

    }

    @Test
    public void testReadAllContent() throws Exception {
        mStore.open();
        RootFolder root = mStore.getDefaultFolder();

        Collection<? extends ConversationHistoryFolder> folders = root
                .getConversationHistoryFolders();
        int count = folders.size();

        assertEquals(2, count);

        List<String> conversationIds = new ArrayList<String>();

        for (ConversationHistoryFolder conversationHistoryFolder : folders) {
            conversationIds.add(conversationHistoryFolder.getName());
        }

        assertTrue(conversationIds.contains("conv1"));
        assertTrue(conversationIds.contains("conv2"));

        ConversationHistoryFolder conv1 = root.getConversationHistoryFolder("conv1");
        Set<CpmObject> messages = conv1.getCpmObjects();
        Set<? extends SessionHistoryFolder> sessions = conv1.getSessionHistoryFolders();
        for (SessionHistoryFolder sessionHistoryFolder : sessions) {
            messages.addAll(sessionHistoryFolder.getCpmObjects());
        }
        assertEquals(2, messages.size());

        Iterator<CpmObject> iter = messages.iterator();
        CpmObject obj1 = iter.next();
        CpmObject obj2 = iter.next();

        assertTrue(obj1 instanceof CpmChatMessage);
        assertTrue(obj2 instanceof CpmChatMessage);

        CpmChatMessage chatMessage1 = (CpmChatMessage) obj1;
        CpmChatMessage chatMessage2 = (CpmChatMessage) obj2;

        // assertEquals("Hello World!", chatMessage1.getText());

        // assertEquals("Hello World In Session!", chatMessage2.getText());
        assertEquals("conv1", chatMessage1.getConversationId());
        assertEquals("conv1", chatMessage2.getConversationId());

        // assertEquals("sess1", chatMessage2.getContributionId());

    }

    @After
    public void tearDown() throws Exception {
        if (mStore != null) {
            mStore.close();
        }
    }
}
