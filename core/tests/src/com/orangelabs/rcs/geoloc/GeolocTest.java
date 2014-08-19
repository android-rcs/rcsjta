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
package com.orangelabs.rcs.geoloc;

import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;

import android.test.AndroidTestCase;

public class GeolocTest extends AndroidTestCase {
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public final void testGeolocPush() {
		GeolocPush g = new GeolocPush("label", 1.0, 2.0,  0, 100);
		assertEquals(g.getLabel(), "label");
		assertEquals(g.getLatitude(), 1.0);
		assertEquals(g.getLongitude(), 2.0);
		assertEquals(g.getExpiration(), 0);
		assertEquals((int)(g.getAccuracy() + 0.4), 100);
	}

}
