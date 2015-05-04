
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User12Test extends AbstractUserTest {

    public User12Test() {
        super("test1000sOfMessages");
    }

    @Override
    protected void setUp() throws Exception {
        prepareImap = false; // disable populate imap
        super.setUp();
    }

    public void test1000sOfMessages() throws Exception {
        long now = System.currentTimeMillis();
        executeSync();
        System.out.println("Executed : " + ((System.currentTimeMillis() - now) / 1000) + "seconds");
    }

}
