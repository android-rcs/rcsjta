/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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