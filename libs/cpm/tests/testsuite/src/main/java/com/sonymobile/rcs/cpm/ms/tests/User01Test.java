
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User01Test extends AbstractUserTest {

    public User01Test() {
        super("testChatMessages");
    }

    public void testChatMessages() throws Exception {
        assertTrue(executeSync());

        // check standalone
        // assertTrue(chatMessageExists("1000"));

        // String m1 = getChatMessageContent("1000");
        // assertEquals("Hello World!", m1);

        // check in session
        assertTrue(db.chatMessageExists("1234"));
        assertEquals("Hello World in Session!", db.getChatMessageContent("1234"));
        assertEquals("sess11", db.getChatMessageChatId("1234"));
        assertTrue(db.groupChatExists("sess11"));
        assertEquals("This is an optional subject", db.getGroupChatSubject("sess11"));

        // check in session other message
        assertTrue(db.chatMessageExists("1235"));
        assertEquals("Another message in another session", db.getChatMessageContent("1235"));
        assertEquals("sess2", db.getChatMessageChatId("1235"));
        assertTrue(db.groupChatExists("sess2"));
        assertEquals("This is another optional subject", db.getGroupChatSubject("sess2"));

        // check timestamps
        assertEquals(asTimestamp(2014, 11, 5, 18, 8, 11, "GMT+1"),
                db.getGroupChatTimestamp("sess11"));
        assertEquals(asTimestamp(2014, 11, 5, 18, 8, 11, "GMT+1"),
                db.getGroupChatTimestamp("sess2"));
        // assertEquals(asTimestamp(2014, 6, 9, 12, 2, 2, "GMT+2"),
        // db.getChatMessageTimestamp("1000"));

        // Fri, 21 Oct 2014 10:01:10 -0600
        assertEquals(asTimestamp(2014, 9, 21, 10, 1, 10, "GMT-6"),
                db.getChatMessageTimestamp("1234"));

        // Fri, 21 Oct 2014 10:01:10 -0600
        assertEquals(asTimestamp(2014, 9, 21, 10, 1, 10, "GMT-6"),
                db.getChatMessageTimestamp("1235"));
    }

}
