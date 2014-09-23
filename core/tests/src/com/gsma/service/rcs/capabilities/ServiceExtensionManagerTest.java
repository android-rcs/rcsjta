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
package com.gsma.service.rcs.capabilities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import android.test.AndroidTestCase;
import android.util.Log;

import com.orangelabs.rcs.core.ims.service.extension.ServiceExtensionManager;

public class ServiceExtensionManagerTest extends AndroidTestCase {

	private Set<String> extensions;

	protected void setUp() throws Exception {
		super.setUp();
		extensions = new HashSet<String>();
		extensions.add(UUID.randomUUID().toString());
		extensions.add(UUID.randomUUID().toString());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetExtensions() {
		String concatenatedExtensions = ServiceExtensionManager.getInstance().getExtensions(extensions);
		Log.d("RCS", "testGetExtensions concatenatedExtensions=" + concatenatedExtensions);
		Set<String> newExtensions = ServiceExtensionManager.getInstance().getExtensions(concatenatedExtensions);
		assertEquals(newExtensions, extensions);
	}
	
	public void testGetExtensionsEmptyTokens() {
		Set<String> newExtensions = ServiceExtensionManager.getInstance().getExtensions("; ;");
		assertTrue(newExtensions.isEmpty());
	}

}