package com.gsma.rcs.api;

import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;

import junit.framework.TestSuite;

public class CodeSamplesInstrumentationTestRunner extends InstrumentationTestRunner {

    @Override
    public TestSuite getAllTests() {
        InstrumentationTestSuite suite = new InstrumentationTestSuite(this);

        suite.addTestSuite(ContactSampleTest.class);
        return suite;
    }

    @Override
    public ClassLoader getLoader() {
        return CodeSamplesInstrumentationTestRunner.class.getClassLoader();
    }
}
