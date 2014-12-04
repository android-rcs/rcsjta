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
package com.gsma.service.rcs.sharing.image;

import java.util.Random;

import com.gsma.services.rcs.ish.ImageSharingServiceConfiguration;

import android.os.Parcel;
import android.test.AndroidTestCase;

/**
 * @author Danielle Rouquier
 *
 */
public class ImageSharingServiceConfigurationTest extends AndroidTestCase {
	Random random = new Random();
	private long maxSize;

	protected void setUp() throws Exception {
		super.setUp();
		maxSize = random.nextLong();
	}

	public void testImageSharingServiceConfiguration() {
		ImageSharingServiceConfiguration ishConf = new ImageSharingServiceConfiguration(maxSize);
		Parcel parcel = Parcel.obtain();
		ishConf.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		ImageSharingServiceConfiguration createFromParcel = ImageSharingServiceConfiguration.CREATOR
				.createFromParcel(parcel);
		assertEquals(createFromParcel.getMaxSize(), ishConf.getMaxSize());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
