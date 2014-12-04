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
package com.gsma.service.rcs.extension;

import java.util.Random;

import com.gsma.services.rcs.extension.MultimediaSessionServiceConfiguration;

import android.os.Parcel;
import android.test.AndroidTestCase;

/**
 * @author Danielle Rouquier
 *
 */
public class MultimediaSessionServiceConfigurationTest extends AndroidTestCase {
	private int tMaxMsgLength;
	private Random random = new Random();

	protected void setUp() throws Exception {
		super.setUp();
		tMaxMsgLength = random.nextInt();
	}

	public void testMultimediaSessionServiceConfiguration() {
		MultimediaSessionServiceConfiguration mmssConf = new MultimediaSessionServiceConfiguration(tMaxMsgLength);
		Parcel parcel = Parcel.obtain();
		mmssConf.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		MultimediaSessionServiceConfiguration createFromParcel = MultimediaSessionServiceConfiguration.CREATOR
				.createFromParcel(parcel);
		assertEquals(createFromParcel.getMessageMaxLength(), mmssConf.getMessageMaxLength());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
