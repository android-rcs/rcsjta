
package com.sonymobile.rcs.cpm.ms.tests;

import com.sonymobile.rcs.cpm.ms.tests.util.AbstractUserTest;

public class User10Test extends AbstractUserTest {

    public User10Test() {
        super("testRepeatSync10Messages");
    }

    @Override
    protected void setUp() throws Exception {
        prepareImap = false;
        super.setUp();
    }

    public void testRepeatSync10Messages() throws Exception {
        executeSync();

        Thread.sleep(1000);

        executeSync();

        //
    }

}
