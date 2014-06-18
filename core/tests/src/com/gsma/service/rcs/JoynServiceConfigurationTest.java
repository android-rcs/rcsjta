package com.gsma.service.rcs;

import junit.framework.Assert;

import com.gsma.services.rcs.JoynServiceConfiguration;
import com.orangelabs.rcs.provider.settings.RcsSettings;

import android.test.AndroidTestCase;

public class JoynServiceConfigurationTest extends AndroidTestCase {

	private RcsSettings rcsSettings;
	
	protected void setUp() throws Exception {
		super.setUp();
		RcsSettings.createInstance(mContext);
		rcsSettings = RcsSettings.getInstance();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSetMyDisplayNameNull() {
		try {
			JoynServiceConfiguration.setMyDisplayName(mContext, null);
			Assert.fail("Expecting exception but none was thrown.");
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException == false) {
				Assert.fail("Exception was thrown was unexpected.");
			}
		}
	}

	public void testSetMyDisplayName() {
		String displayNameSav = rcsSettings.getUserProfileImsDisplayName();
		JoynServiceConfiguration.setMyDisplayName(mContext, "mydisplayname");
		Assert.assertEquals(JoynServiceConfiguration.getMyDisplayName(mContext), "mydisplayname");
		rcsSettings.setUserProfileImsDisplayName(displayNameSav);
	}

	public void testSsetMyDisplayNameWithBlank() {
		String displayNameSav = rcsSettings.getUserProfileImsDisplayName();
		JoynServiceConfiguration.setMyDisplayName(mContext, "my display name");
		Assert.assertEquals(JoynServiceConfiguration.getMyDisplayName(mContext), "my display name");
		rcsSettings.setUserProfileImsDisplayName(displayNameSav);
	}

	public void testSetMyDisplayNameEmpty() {
		String displayNameSav = rcsSettings.getUserProfileImsDisplayName();
		JoynServiceConfiguration.setMyDisplayName(mContext, "");
		Assert.assertEquals(JoynServiceConfiguration.getMyDisplayName(mContext), "");
		rcsSettings.setUserProfileImsDisplayName(displayNameSav);
	}

	public void testDefaultMessagingMethod() {
		int defaultMessaginMethod = rcsSettings.getDefaultMessagingMethod();
		JoynServiceConfiguration.setDefaultMessagingMethod(mContext, JoynServiceConfiguration.Settings.DefaultMessagingMethods.AUTOMATIC);
		assertEquals(JoynServiceConfiguration.Settings.DefaultMessagingMethods.AUTOMATIC,
				JoynServiceConfiguration.getDefaultMessagingMethod(mContext));
		JoynServiceConfiguration.setDefaultMessagingMethod(mContext, JoynServiceConfiguration.Settings.DefaultMessagingMethods.JOYN);
		assertEquals(JoynServiceConfiguration.Settings.DefaultMessagingMethods.JOYN,
				JoynServiceConfiguration.getDefaultMessagingMethod(mContext));
		rcsSettings.setDefaultMessagingMethod(defaultMessaginMethod);
	}

	public void testSetDefaultMessagingInvalidArgument() {
		try {
			JoynServiceConfiguration.setDefaultMessagingMethod(mContext, 4);
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException == false) {
				Assert.fail("Exception was thrown was unexpected.");
			}
		}
	}

	public void testGetMyCountryCode() {
		String ccSav = rcsSettings.getCountryCode();
		rcsSettings.setCountryCode("+44");
		assertEquals("+44", JoynServiceConfiguration.getMyCountryCode(mContext));
		rcsSettings.setCountryCode(ccSav);
	}
	
	public void testGetMyContactId() {
		String contactIdSav = rcsSettings.getUserProfileImsUserName();
		rcsSettings.setUserProfileImsUserName("+33123456789");
		assertEquals("+33123456789", JoynServiceConfiguration.getMyContactId(mContext));
		rcsSettings.setUserProfileImsUserName(contactIdSav);
	}
	
	public void testIsConfigValid() {
		boolean isConfigValidSav = rcsSettings.isConfigurationValid();
		rcsSettings.setConfigurationValid(false);
		assertFalse(JoynServiceConfiguration.isConfigValid(mContext));
		rcsSettings.setConfigurationValid(true);
		assertTrue(JoynServiceConfiguration.isConfigValid(mContext));
		rcsSettings.setConfigurationValid(isConfigValidSav);
	}
	
	public void testGetMessaginUX() {
		int getMessagingUXSav  = rcsSettings.getMessagingMode();
		rcsSettings.setMessagingMode(JoynServiceConfiguration.Settings.MessagingModes.CONVERGED);
		assertEquals(JoynServiceConfiguration.Settings.MessagingModes.CONVERGED,
				JoynServiceConfiguration.getMessagingUX(mContext));
		rcsSettings.setMessagingMode(JoynServiceConfiguration.Settings.MessagingModes.SEAMLESS);
		assertEquals(JoynServiceConfiguration.Settings.MessagingModes.SEAMLESS,
				JoynServiceConfiguration.getMessagingUX(mContext));
		rcsSettings.setMessagingMode(getMessagingUXSav);
	}
	

}
