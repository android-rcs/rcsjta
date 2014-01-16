package android.tests.provider;

import junit.framework.TestSuite;
import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;

public class RcsApiProviderInstrumentationTestRunner  extends InstrumentationTestRunner {

	    @Override
	    public TestSuite getAllTests() {
	        InstrumentationTestSuite suite = new InstrumentationTestSuite(this);

	        suite.addTestSuite(CapabilitiesLogTest.class);
	        suite.addTestSuite(ChatLogGroupChatTest.class);
	        suite.addTestSuite(ChatLogMessageTest.class);
	        suite.addTestSuite(FileTransferLogTest.class);
	        suite.addTestSuite(ImageSharingLogTest.class);
	        suite.addTestSuite(IPCallLogTest.class);
	        suite.addTestSuite(VideoSharingLogTest.class);
	        return suite;
	    }

	    @Override
	    public ClassLoader getLoader() {
	        return RcsApiProviderInstrumentationTestRunner.class.getClassLoader();
	    }
	}