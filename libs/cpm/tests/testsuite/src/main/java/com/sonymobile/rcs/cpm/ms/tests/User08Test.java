
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User08Test extends AbstractUserTest {

    public User08Test() {
        super("testSetRemoteAsSeenWhenLocalIsRead");
    }

    public void testSetRemoteAsSeenWhenLocalIsRead() throws Exception {
        // insert group chat
        db.insertGroupChat("abcd", 1, "", 1, 1, 1, "");

        db.insertChatMessage("80000", "abcd", "", "", 0, "text/plain", 1, 1, 1, 1); // SEEN = TRUE
        db.insertChatMessage("80001", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);

        // insert two 1-1 chat messages
        db.insertChatMessage("80002", "abcd", "", "", 0, "text/plain", 1, 1, 1, 1); // TRUE
        db.insertChatMessage("80003", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);

        assertTrue(db.chatMessageExists("80000"));
        assertTrue(db.chatMessageExists("80001"));
        assertTrue(db.chatMessageExists("80002"));
        assertTrue(db.chatMessageExists("80003"));

        // check that the remote message is not Seen

        assertTrue(db.isChatMessageRead("80000"));
        // assertFalse(isImapMessageSeen("80000", "e5f6g7h8i9"));

        executeSync();

        assertTrue(imap.isMessageSeen("80000", "e5f6g7h8i9"));
        assertFalse(imap.isMessageSeen("80001", "e5f6g7h8i9"));
        assertTrue(imap.isMessageSeen("80002", "e5f6g7h8i9"));
        assertFalse(imap.isMessageSeen("80003", "e5f6g7h8i9"));

    }

}
