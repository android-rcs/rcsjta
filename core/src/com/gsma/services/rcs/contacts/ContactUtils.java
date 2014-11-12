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

package com.gsma.services.rcs.contacts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsServiceConfiguration;

import android.content.Context;
import android.text.TextUtils;

/**
 * ContactUtils class for validation and unique formatting of phone numbers
 * 
 * @author YPLO6403
 * 
 */
public class ContactUtils {

	/**
	 * Singleton of ContactUtils
	 */
	private static volatile ContactUtils instance = null;

	/**
	 * The country code of the device (read from settings provider: is null before first provisioning)
	 */
	private String mCountryCode = null;

	/**
	 * The country area code(read from settings provider)
	 */
	private String mCountryAreaCode = null;

	/**
	 * Regular expression to validate phone numbers
	 */
	private final static String REGEXP_CONTACT = "^00\\d{1,15}$|^[+]?\\d{1,15}$|^\\d{1,15}$";

	/**
	 * Regular expression to validate Country Codes
	 */
	private final static String REGEXP_COUNTRY_CODE = "^\\+\\d{1,3}$";

	/**
	 * Pattern to check contact
	 */
	private final static Pattern PATTERN_CONTACT = Pattern.compile(REGEXP_CONTACT);

	/**
	 * Pattern to check Country Code
	 */
	private final static Pattern PATTERN_COUNTRY_CODE = Pattern.compile(REGEXP_COUNTRY_CODE);

	private final static String MSISDN_PREFIX_INTERNATIONAL = "00";

	private String msisdnWithPrefixAndCountryCode;

	/**
	 * Empty constructor : prevent caller from creating multiple instances
	 */
	private ContactUtils() {
	}

	/**
	 * Get an instance of ContactUtils.
	 * 
	 * @param context
	 *            the context
	 * @return the singleton instance. May be null if country code cannot be read from provider.
	 */
	public static ContactUtils getInstance(Context context) {
		if (instance == null) {
			synchronized (ContactUtils.class) {
				if (instance == null) {
					if (context == null) {
						throw new IllegalArgumentException("Context is null");
					}
					String countryCode = RcsServiceConfiguration.getMyCountryCode(context);
					if (countryCode != null) {
						// Check for Country Code validity
						Matcher matcher = PATTERN_COUNTRY_CODE.matcher(countryCode);
						if (matcher.find()) {
							instance = new ContactUtils();
							instance.mCountryCode = countryCode;
							instance.mCountryAreaCode = RcsServiceConfiguration.getMyCountryAreaCode(context);
							instance.msisdnWithPrefixAndCountryCode = new StringBuilder(MSISDN_PREFIX_INTERNATIONAL).append(
									countryCode.substring(1)).toString();
						}
					}
				}
			}
		}
		return instance;
	}

	/**
	 * Remove blank and minus characters from contact
	 * 
	 * @param contact
	 *            the phone number
	 * @return phone string stripped of separators.
	 */
	private String stripSeparators(String contact) {
		if (TextUtils.isEmpty(contact)) {
			return null;
		}
		contact = contact.replaceAll("[ -]", "");
		Matcher matcher = PATTERN_CONTACT.matcher(contact);
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

	/**
	 * Verify the validity of a contact number.
	 * 
	 * @param contact
	 *            the contact number
	 * @return Returns true if the given ContactId have the syntax of valid Rcs ContactId.
	 */
	public boolean isValidContact(String contact) {
		return (!TextUtils.isEmpty(stripSeparators(contact)));
	}

	/**
	 * Formats the given contact to uniquely represent a Rcs contact phone number.
	 * <p>
	 * May throw a RcsContactFormatException exception if the string contact parameter is not enabled to produce a valid ContactId.
	 * 
	 * @param contact
	 *            the contact phone number
	 * @return the ContactId
	 */
	public ContactId formatContact(String contact) {
		contact = stripSeparators(contact);
		if (!TextUtils.isEmpty(contact)) {
			// Is Country Code provided ?
			if (contact.charAt(0) != '+') {
				// CC not provided, does it exists in provider ?
				if (mCountryCode == null) {
					throw new RcsContactFormatException("Country code is unknown");
				}
				// International numbering with prefix ?
				if (contact.startsWith(msisdnWithPrefixAndCountryCode)) {
					contact = new StringBuilder(mCountryCode).append(contact.substring(msisdnWithPrefixAndCountryCode.length()))
							.toString();
				} else {
					// Local numbering ?
					if (!TextUtils.isEmpty(mCountryAreaCode)) {
						// Local number must start with Country Area Code
						if (contact.startsWith(mCountryAreaCode)) {
							// Remove Country Area Code and add Country Code
							contact = new StringBuilder(mCountryCode).append(contact.substring(mCountryAreaCode.length())).toString();
						} else {
							throw new RcsContactFormatException("Local phone number should be prefixed with Country Area Code");
						}
					} else {
						// No Country Area Code, add Country code to local number
						contact = new StringBuilder(mCountryCode).append(contact).toString();
					}
				}
			}
			return new ContactId(contact);
		}
		throw new RcsContactFormatException("Input parameter is null or empty");
	}

}
