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

import junit.framework.Assert;

import com.gsma.services.rcs.JoynContactFormatException;
import com.gsma.services.rcs.JoynServiceConfiguration;
import com.orangelabs.rcs.provider.settings.RcsSettings;

import android.test.AndroidTestCase;

public class JoynServiceConfigurationTest extends AndroidTestCase {

	private RcsSettings rcsSettings;
	
	protected void setUp() throws Exception {
		super.setUp();
		RcsSettings.createInstance(getContext());
		rcsSettings = RcsSettings.getInstance();
		assertNotNull("Cannot instantiate RcsSettings", rcsSettings);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSetMyDisplayNameNull() {
		try {
			JoynServiceConfiguration.setMyDisplayName(getContext(), null);
			Assert.fail("Expecting exception but none was thrown.");
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException == false) {
				Assert.fail("Exception thrown was unexpected.");
			}
		}
	}

	public void testSetMyDisplayName() {
		String displayNameSav = rcsSettings.getUserProfileImsDisplayName();
		JoynServiceConfiguration.setMyDisplayName(getContext(), "mydisplayname");
		Assert.assertEquals(JoynServiceConfiguration.getMyDisplayName(getContext()), "mydisplayname");
		rcsSettings.setUserProfileImsDisplayName(displayNameSav);
	}

	public void testSsetMyDisplayNameWithBlank() {
		String displayNameSav = rcsSettings.getUserProfileImsDisplayName();
		JoynServiceConfiguration.setMyDisplayName(getContext(), "my display name");
		Assert.assertEquals(JoynServiceConfiguration.getMyDisplayName(getContext()), "my display name");
		rcsSettings.setUserProfileImsDisplayName(displayNameSav);
	}

	public void testSetMyDisplayNameEmpty() {
		String displayNameSav = rcsSettings.getUserProfileImsDisplayName();
		JoynServiceConfiguration.setMyDisplayName(getContext(), "");
		Assert.assertEquals(JoynServiceConfiguration.getMyDisplayName(getContext()), "");
		rcsSettings.setUserProfileImsDisplayName(displayNameSav);
	}

	public void testDefaultMessagingMethod() {
		int defaultMessaginMethod = rcsSettings.getDefaultMessagingMethod();
		JoynServiceConfiguration.setDefaultMessagingMethod(getContext(), JoynServiceConfiguration.Settings.DefaultMessagingMethods.AUTOMATIC);
		assertEquals(JoynServiceConfiguration.Settings.DefaultMessagingMethods.AUTOMATIC,
				JoynServiceConfiguration.getDefaultMessagingMethod(getContext()));
		JoynServiceConfiguration.setDefaultMessagingMethod(getContext(), JoynServiceConfiguration.Settings.DefaultMessagingMethods.RCS);
		assertEquals(JoynServiceConfiguration.Settings.DefaultMessagingMethods.RCS,
				JoynServiceConfiguration.getDefaultMessagingMethod(getContext()));
		rcsSettings.setDefaultMessagingMethod(defaultMessaginMethod);
	}

	public void testSetDefaultMessagingInvalidArgument() {
		try {
			JoynServiceConfiguration.setDefaultMessagingMethod(getContext(), 4);
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException == false) {
				Assert.fail("Exception thrown was unexpected.");
			}
		}
	}

	public void testIsServiceActivated() {
		boolean serviceActivated = rcsSettings.isServiceActivated();
		assertEquals(serviceActivated, JoynServiceConfiguration.isServiceActivated(getContext()));
	}
	
	public void testGetMyCountryCode() {
		String countryCode = rcsSettings.getCountryCode();
		assertEquals(countryCode, JoynServiceConfiguration.getMyCountryCode(getContext()));
	}
	
	public void testGetMyCountryAreaCode() {
		String countryAreaCode = rcsSettings.getCountryAreaCode();
		assertEquals(countryAreaCode, JoynServiceConfiguration.getMyCountryAreaCode(getContext()));
	}
	
	public void testGetMyContactId() {
		String contactIdSav = rcsSettings.getUserProfileImsUserName();
		rcsSettings.setUserProfileImsUserName("+33123456789");
		try {
			assertEquals("+33123456789", JoynServiceConfiguration.getMyContactId(getContext()).toString());
		} catch (JoynContactFormatException e) {
			fail("Failed to getMyContactId");
		}
		rcsSettings.setUserProfileImsUserName(contactIdSav);
	}
	
	public void testGetMyContactIdWithoutCC() {
		String cac = rcsSettings.getCountryAreaCode();
		String contactIdSav = rcsSettings.getUserProfileImsUserName();
		rcsSettings.setUserProfileImsUserName(cac+"23456789");
		String cc = rcsSettings.getCountryCode();
		try {
			assertEquals(cc+"23456789",JoynServiceConfiguration.getMyContactId(getContext()).toString());
		} catch (JoynContactFormatException e) {
			fail("Failed to testGetMyContactIdWithoutCC");
		}
		rcsSettings.setUserProfileImsUserName(contactIdSav);
	}
	
	public void testGetMyContactIdBadFormat() {
		String contactIdSav = rcsSettings.getUserProfileImsUserName();
		rcsSettings.setUserProfileImsUserName("1234w56789");
		try {
			JoynServiceConfiguration.getMyContactId(getContext());
			fail("Expecting exception but none was thrown.");
		} catch (JoynContactFormatException e) {
			if (e instanceof JoynContactFormatException == false) {
				Assert.fail("Exception thrown was unexpected.");
			}
		}
		rcsSettings.setUserProfileImsUserName(contactIdSav);
	}
	
	public void testIsConfigValid() {
		boolean isConfigValidSav = rcsSettings.isConfigurationValid();
		rcsSettings.setConfigurationValid(false);
		assertFalse(JoynServiceConfiguration.isConfigValid(getContext()));
		rcsSettings.setConfigurationValid(true);
		assertTrue(JoynServiceConfiguration.isConfigValid(getContext()));
		rcsSettings.setConfigurationValid(isConfigValidSav);
	}
	
	public void testGetMessaginUX() {
		int getMessagingUXSav  = rcsSettings.getMessagingMode();
		rcsSettings.setMessagingMode(JoynServiceConfiguration.Settings.MessagingModes.CONVERGED);
		assertEquals(JoynServiceConfiguration.Settings.MessagingModes.CONVERGED,
				JoynServiceConfiguration.getMessagingUX(getContext()));
		rcsSettings.setMessagingMode(JoynServiceConfiguration.Settings.MessagingModes.SEAMLESS);
		assertEquals(JoynServiceConfiguration.Settings.MessagingModes.SEAMLESS,
				JoynServiceConfiguration.getMessagingUX(getContext()));
		rcsSettings.setMessagingMode(getMessagingUXSav);
	}
	
}