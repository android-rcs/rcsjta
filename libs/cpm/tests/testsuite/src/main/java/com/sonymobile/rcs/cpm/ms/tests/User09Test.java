
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User09Test extends AbstractUserTest {

    public User09Test() {
        super("testSetLocalReadWhenRemoteIsSeen");
    }

    public void testSetLocalReadWhenRemoteIsSeen() throws Exception {
        // insert group chat
        db.insertGroupChat("abcd", 1, "", 1, 1, 1, "");

        db.insertChatMessage("90000", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);
        db.insertChatMessage("90001", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);

        // insert two 1-1 chat messages
        db.insertChatMessage("90002", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);
        db.insertChatMessage("90003", "abcd", "", "", 0, "text/plain", 1, 1, 0, 1);

        assertTrue(db.chatMessageExists("90000"));
        assertTrue(db.chatMessageExists("90001"));
        assertTrue(db.chatMessageExists("90002"));
        assertTrue(db.chatMessageExists("90003"));

        assertFalse(db.isChatMessageRead("90000"));
        assertFalse(db.isChatMessageRead("90001"));
        assertFalse(db.isChatMessageRead("90002"));
        assertFalse(db.isChatMessageRead("90003"));

        executeSync();

        assertFalse(db.isChatMessageRead("90000"));
        assertFalse(db.isChatMessageRead("90001"));
        assertFalse(db.isChatMessageRead("90002"));
        assertFalse(db.isChatMessageRead("90003"));

        imap.setMessageSeen("90000", "e5f6g7h8i9");
        imap.setMessageSeen("90002", "e5f6g7h8i9");
        imap.doExpunge();

        executeSync();

        assertTrue(db.isChatMessageRead("90000"));
        assertFalse(db.isChatMessageRead("90001"));
        assertTrue(db.isChatMessageRead("90002"));
        assertFalse(db.isChatMessageRead("90003"));

    }

}
