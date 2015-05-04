package com.sonymobile.rcs.core.messagestore.testsuite;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase {

    public AllTests() {
    }
    
    public AllTests(String n) {
        super(n);
    }
    
    
    @SuppressWarnings("unchecked")
    public static Test suite() throws Exception {
        
//        MediatorBuilder mediatorBuilder = new StandaloneMediatorBuilder();
//        
//        TestSuite suite = SuiteFactory.createSuite(mediatorBuilder, 
//                DummyLocalPersistence.getInstance()
//                , new DefaultRemotePersistence());
        TestSuite suite = new TestSuite();
        suite.addTest(new AllTests("doNothing"));
        
        return suite;
    }
    
    public void doNothing(){
        
    }

}
