
package com.sonymobile.rcs.cpm.ms.tests.util;

import com.sonymobile.rcs.cpm.ms.tests.User01Test;

import junit.framework.TestSuite;

public class SuiteFactory {

    @SuppressWarnings("unchecked")
    public static TestSuite createSuite(LocalPersistence local, RemotePersistence remote)
            throws Exception {
        TestSuite suite = new TestSuite("MessageStoreHappyCases");
        // $JUnit-BEGIN$
        for (int i = 1; i < 11; i++) {
            Class<? extends AbstractUserTest> c = (Class<? extends AbstractUserTest>) Class
                    .forName(User01Test.class.getPackage().getName() + ".User"
                            + String.format("%02d", i) + "Test");
            AbstractUserTest t = c.newInstance();
            t.setRemotePersistence(remote);
            // t.setSyncMediator(mediator);
            t.setLocalPersistence(local);
            suite.addTest(t);
        }
        // $JUnit-END$
        return suite;
    }

}
