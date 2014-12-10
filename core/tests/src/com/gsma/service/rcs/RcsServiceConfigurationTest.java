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

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.DefaultMessagingMethod;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.MessagingMode;

import android.content.Context;
import android.test.AndroidTestCase;

public class RcsServiceConfigurationTest extends AndroidTestCase {

	private RcsSettings mRcsSettings;
	private ContactUtils mContactUtils;
	private Context mContext;

	protected void setUp() throws Exception {
		super.setUp();
		mContext = getContext();
		RcsSettings.createInstance(mContext);
		mRcsSettings = RcsSettings.getInstance();
		assertNotNull("Cannot instantiate RcsSettings", mRcsSettings);
		mContactUtils = ContactUtils.getInstance(mContext);
		assertNotNull("Cannot instantiate ContactUtils", mContactUtils);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSetMyDisplayNameNull() {
		try {
			RcsServiceConfiguration.setMyDisplayName(mContext, null);
			Assert.fail("Expecting exception but none was thrown.");
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException == false) {
				Assert.fail("Exception thrown was unexpected.");
			}
		}
	}

	public void testSetMyDisplayName() {
		String displayNameSav = mRcsSettings.getUserProfileImsDisplayName();
		RcsServiceConfiguration.setMyDisplayName(mContext, "mydisplayname");
		Assert.assertEquals(RcsServiceConfiguration.getMyDisplayName(mContext), "mydisplayname");
		mRcsSettings.setUserProfileImsDisplayName(displayNameSav);
	}

	public void testSsetMyDisplayNameWithBlank() {
		String displayNameSav = mRcsSettings.getUserProfileImsDisplayName();
		RcsServiceConfiguration.setMyDisplayName(mContext, "my display name");
		Assert.assertEquals(RcsServiceConfiguration.getMyDisplayName(mContext),
				"my display name");
		mRcsSettings.setUserProfileImsDisplayName(displayNameSav);
	}

	public void testSetMyDisplayNameEmpty() {
		String displayNameSav = mRcsSettings.getUserProfileImsDisplayName();
		RcsServiceConfiguration.setMyDisplayName(mContext, "");
		Assert.assertEquals(RcsServiceConfiguration.getMyDisplayName(mContext), "");
		mRcsSettings.setUserProfileImsDisplayName(displayNameSav);
	}

	public void testDefaultMessagingMethod() {
		DefaultMessagingMethod defaultMessaginMethod = mRcsSettings.getDefaultMessagingMethod();
		RcsServiceConfiguration.setDefaultMessagingMethod(mContext,
				RcsServiceConfiguration.Settings.DefaultMessagingMethods.AUTOMATIC);
		assertEquals(RcsServiceConfiguration.Settings.DefaultMessagingMethods.AUTOMATIC,
				RcsServiceConfiguration.getDefaultMessagingMethod(mContext));
		RcsServiceConfiguration.setDefaultMessagingMethod(mContext,
				RcsServiceConfiguration.Settings.DefaultMessagingMethods.RCS);
		assertEquals(RcsServiceConfiguration.Settings.DefaultMessagingMethods.RCS,
				RcsServiceConfiguration.getDefaultMessagingMethod(mContext));
		mRcsSettings.setDefaultMessagingMethod(defaultMessaginMethod);
	}

	public void testSetDefaultMessagingInvalidArgument() {
		try {
			RcsServiceConfiguration.setDefaultMessagingMethod(mContext, 4);
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException == false) {
				Assert.fail("Exception thrown was unexpected.");
			}
		}
	}

	public void testGetMyContactId() {
		String contactIdSav = mRcsSettings.getUserProfileImsUserName();
		mRcsSettings.setUserProfileImsUserName("+33123456789");
		try {
			assertEquals("+33123456789", RcsServiceConfiguration.getMyContactId(mContext)
					.toString());
		} catch (RcsContactFormatException e) {
			fail("Failed to getMyContactId");
		}
		mRcsSettings.setUserProfileImsUserName(contactIdSav);
	}

	public void testGetMyContactIdWithoutCC() {
		String cac = mContactUtils.getMyCountryAreaCode();
		String contactIdSav = mRcsSettings.getUserProfileImsUserName();
		mRcsSettings.setUserProfileImsUserName(cac + "23456789");
		String cc = mContactUtils.getMyCountryCode();
		try {
			assertEquals(cc + "23456789", RcsServiceConfiguration.getMyContactId(mContext)
					.toString());
		} catch (RcsContactFormatException e) {
			fail("Failed to testGetMyContactIdWithoutCC");
		}
		mRcsSettings.setUserProfileImsUserName(contactIdSav);
	}

	public void testGetMyContactIdBadFormat() {
		String contactIdSav = mRcsSettings.getUserProfileImsUserName();
		mRcsSettings.setUserProfileImsUserName("1234w56789");
		try {
			RcsServiceConfiguration.getMyContactId(mContext);
			fail("Expecting exception but none was thrown.");
		} catch (RcsContactFormatException e) {
			if (e instanceof RcsContactFormatException == false) {
				Assert.fail("Exception thrown was unexpected.");
			}
		}
		mRcsSettings.setUserProfileImsUserName(contactIdSav);
	}

	public void testIsConfigValid() {
		boolean isConfigValidSav = mRcsSettings.isConfigurationValid();
		mRcsSettings.setConfigurationValid(false);
		assertFalse(RcsServiceConfiguration.isConfigValid(mContext));
		mRcsSettings.setConfigurationValid(true);
		assertTrue(RcsServiceConfiguration.isConfigValid(mContext));
		mRcsSettings.setConfigurationValid(isConfigValidSav);
	}

	public void testGetMessaginUX() {
		MessagingMode getMessagingUXSav = mRcsSettings.getMessagingMode();
		mRcsSettings.setMessagingMode(MessagingMode.CONVERGED);
		assertEquals(MessagingMode.CONVERGED.ordinal(),
				RcsServiceConfiguration.getMessagingUX(mContext));
		mRcsSettings.setMessagingMode(MessagingMode.SEAMLESS);
		assertEquals(MessagingMode.SEAMLESS.ordinal(),
				RcsServiceConfiguration.getMessagingUX(mContext));
		mRcsSettings.setMessagingMode(getMessagingUXSav);
	}


}
