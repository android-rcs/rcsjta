
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User07Test extends AbstractUserTest {

    public User07Test() {
        super("testDeleteRemotelyWhenMessageIsDeletedLocally");
    }

    public void testDeleteRemotelyWhenMessageIsDeletedLocally() throws Exception {
        // insert group chat
        db.insertGroupChat("abcd", 1, "", 1, 1, 1, "");

        db.insertChatMessage("70000", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);
        db.insertChatMessage("70001", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);

        // insert two 1-1 chat messages
        db.insertChatMessage("70002", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);
        db.insertChatMessage("70003", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);

        assertTrue(db.chatMessageExists("70000"));
        assertTrue(db.chatMessageExists("70001"));
        assertTrue(db.chatMessageExists("70002"));
        assertTrue(db.chatMessageExists("70003"));

        executeSync();

        db.deleteChatMessage("70001");
        db.deleteChatMessage("70003");

        executeSync();

        assertTrue(imap.isMessageDeleted("7001", "e5f6g7h8i9"));
        assertTrue(imap.isMessageDeleted("7002", "e5f6g7h8i9"));

    }

}
