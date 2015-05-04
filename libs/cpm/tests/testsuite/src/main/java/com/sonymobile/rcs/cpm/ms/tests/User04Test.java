
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User04Test extends AbstractUserTest {

    public User04Test() {
        super("testMediaObjects");
    }

    public void testMediaObjects() throws Exception {
        executeSync();

        assertTrue(db.chatMessageExists("4444"));

        String content = db.getChatMessageContent("4444");
        byte[] data = decodeBase64(content);

        assertEquals(1101, data.length);

        // Fri, 05 Dec 2014 18:10:23 +0100
        assertEquals(asTimestamp(2014, 11, 5, 18, 10, 23, "GMT+1"),
                db.getChatMessageTimestamp("4444"));

        assertEquals("image/jpeg", db.getChatMessageMimeType("4444"));
    }

}
