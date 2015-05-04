
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User02Test extends AbstractUserTest {

    public User02Test() {
        super("testFileTransfer");
    }

    public void testFileTransfer() throws Exception {
        executeSync();

        long size = 1101;
        String ftId = "33333";

        assertTrue(db.fileTransferExists("33333"));

        assertEquals(null, db.getFileTransferFileName("33333"));
        assertEquals("1234@sonymobile.com", db.getFileTransferContact("33333"));
        assertEquals(null, db.getFileTransferFileUri("33333"));
        // assertEquals(size, getFileTransferFileSize("33333"));
        // assertEquals("", getFileTransferMimeType("33333"));
        // assertEquals(2222, getFileTransferTimestamp("33333"));
        // assertEquals("", getFileTransferChatId("33333"));

        // String content = getFileTransferContent(ftId);

    }

}
