
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User06Test extends AbstractUserTest {

    public User06Test() {
        super("testDeleteLocallyIfRemoteIdIsMissing");
    }

    public void testDeleteLocallyIfRemoteIdIsMissing() throws Exception {
        // insert group chat
        db.insertGroupChat("abcd", 1, "", 1, 1, 1, "");

        db.insertChatMessage("60000", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);
        db.insertChatMessage("60001", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);

        // insert two 1-1 chat messages
        db.insertChatMessage("60002", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);
        db.insertChatMessage("60003", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);

        assertTrue(db.chatMessageExists("60000"));
        assertTrue(db.chatMessageExists("60001"));
        assertTrue(db.chatMessageExists("60002"));
        assertTrue(db.chatMessageExists("60003"));

        executeSync();

        imap.setMessageDeleted("60001", "e5f6g7h8i9");
        imap.setMessageDeleted("60003", "e5f6g7h8i9");
        imap.doExpunge();

        executeSync();

        assertTrue(db.chatMessageExists("60000")); // exists in server
        assertFalse(db.chatMessageExists("60001")); // REMOVED
        assertTrue(db.chatMessageExists("60002")); // exists in server
        assertFalse(db.chatMessageExists("60003")); // REMOVED
    }

}
