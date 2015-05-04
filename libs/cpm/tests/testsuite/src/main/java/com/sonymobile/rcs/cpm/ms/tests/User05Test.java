
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User05Test extends AbstractUserTest {

    public User05Test() {
        super("testExistingChatIdsAreKept");
    }

    public void testExistingChatIdsAreKept() throws Exception {
        long timestamp = System.currentTimeMillis();
        int status = 0;
        int reasonCode = 0;
        String contact = "";
        String content = "the content doesnt really matter";
        int readStatus = 1;
        int direction = 0;

        String participants = "1234@sony.com";
        int state = 0;
        String subject = "the optional subject";

        db.insertGroupChat("555555", state, subject, direction, timestamp, reasonCode, participants);

        db.insertChatMessage("50000", "555555", contact, content, timestamp, "text/plain", status,
                reasonCode, readStatus, direction);

        assertEquals(1, getChatMessageCount());

        executeSync();

        assertEquals(2, getChatMessageCount());

        assertFalse(db.groupChatExists("e5f6g7h8i9"));

        assertEquals("555555", db.getChatMessageChatId("50000"));
        assertEquals("555555", db.getChatMessageChatId("50001"));

    }

    private int getChatMessageCount() {
        return 0;
    }

}
