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
package com.gsma.service.rcs;

import java.util.NoSuchElementException;

import android.test.AndroidTestCase;

import com.gsma.services.rcs.Geoloc;

public class GeolocTest extends AndroidTestCase {

	public void testgetGeolocInvalidData() {
		try {
			new Geoloc("abcd,2,3,4.0");
			fail("Exception is expected");
		} catch (Exception e) {
			assertTrue(e instanceof NumberFormatException);
		}
	}

	public void testgetGeolocMissingData() {
		try {
			new Geoloc("0,1,2");
		}
		catch (Exception e) {
			assertTrue(e instanceof NoSuchElementException);
		}
	}

	public void testgetGeoloc() {
		Geoloc geoloc = new Geoloc("label,1,2,3,4.0");
		assertNotNull(geoloc);
		assertEquals("label", geoloc.getLabel());
		assertEquals(1.0d, geoloc.getLatitude());
		assertEquals(2.0d, geoloc.getLongitude());
		assertEquals(3L, geoloc.getExpiration());
		assertEquals(4.0f, geoloc.getAccuracy());
	}

	public void testgetGeolocNoLabel() {
		Geoloc geoloc = new Geoloc("1,2,3,4.0");
		assertNotNull(geoloc);
		assertEquals(null, geoloc.getLabel());
		assertEquals(1.0d, geoloc.getLatitude());
		assertEquals(2.0d, geoloc.getLongitude());
		assertEquals(3L, geoloc.getExpiration());
		assertEquals(4.0f, geoloc.getAccuracy());
	}

}