
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

import java.util.Arrays;

public class User03Test extends AbstractUserTest {

    public User03Test() {
        super("testGroupStateObjects");
    }

    public void testGroupStateObjects() throws Exception {
        executeSync();

        assertTrue(db.groupChatExists("e5f6g7h8i9"));

        String participants = db.getGroupChatParticipants("e5f6g7h8i9");

        assertNotNull(participants);

        assertTrue(participants.contains("+16135551212"));

        String[] array = participants.split(",");

        assertEquals(3, array.length);

        Arrays.sort(array);

    }

}
