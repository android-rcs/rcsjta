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

package com.orangelabs.rcs.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * Phone utility functions
 * 
 * @author jexa7410
 */
public class PhoneUtils {
	/**
	 * Tel-URI format
	 */
	private static boolean TEL_URI_SUPPORTED = true;

    /**
     * Regular expression of the SIP header
     *
     */
    private final static String REGEXP_EXTRACT_URI = "<(.*)>";
    
    /**
     * Pattern to extract Uri from SIP header
     */
    private final static Pattern PATTERN_EXTRACT_URI = Pattern.compile(REGEXP_EXTRACT_URI);
    
    private static final String TEL_URI_HEADER = "tel:";
    
    private static final String SIP_URI_HEADER = "sip:";

	/**
	 * Set the country code
	 * 
	 * @param context Context
	 */
	public static synchronized void initialize(Context context) {
		RcsSettings.createInstance(context);
		TEL_URI_SUPPORTED = RcsSettings.getInstance().isTelUriFormatUsed();
	}

	/**
	 * Format a phone number to a SIP URI
	 * 
	 * @param number Phone number
	 * @return SIP URI
	 */
	public static String formatNumberToSipUri(String number) {
		if (number == null) {
			return null;
			
		}

		// Remove spaces
		number = number.trim();
		
		// Extract username part
		if (number.startsWith(TEL_URI_HEADER)) {
			number = number.substring(TEL_URI_HEADER.length());
		} else if (number.startsWith(SIP_URI_HEADER)) {
			number = number.substring(SIP_URI_HEADER.length(), number.indexOf("@"));
		}
		try {
			ContactId contact = ContactUtils.createContactId(number);
			number = contact.toString();
			if (TEL_URI_SUPPORTED) {
				// Tel-URI format
				return new StringBuilder(TEL_URI_HEADER).append(number).toString();
				
			} else {
				// SIP-URI format
				return new StringBuilder(SIP_URI_HEADER).append(number).append("@")
						.append(ImsModule.IMS_USER_PROFILE.getHomeDomain()).append(";user=phone").toString();
				
			}
		} catch (RcsContactFormatException e) {
			return null;
			
		}
		
	}
	
	/**
	 * Format ContactId to tel or sip Uri
	 * 
	 * @param contactId
	 *            the contact identifier
	 * @return the Uri
	 */
	public static String formatContactIdToUri(ContactId contactId) {
		if (contactId == null) {
			throw new IllegalArgumentException("ContactId is null");
			
		}
		if (TEL_URI_SUPPORTED) {
			// Tel-URI format
			return new StringBuilder(TEL_URI_HEADER).append(contactId).toString();
			
		} else {
			// SIP-URI format
			return new StringBuilder(SIP_URI_HEADER).append(contactId).append("@").append(ImsModule.IMS_USER_PROFILE.getHomeDomain())
					.append(";user=phone").toString();
			
		}
	}

	/**
	 * Extract user part phone number from a SIP-URI or Tel-URI or SIP address
	 * 
	 * @param uri SIP or Tel URI
	 * @return Unformatted Number or null in case of error
	 */
	public static String extractNumberFromUriWithoutFormatting(String uri) {
		if (uri == null) {
			return null;
			
		}

		try {
			// Extract URI from address
			int index0 = uri.indexOf("<");
			if (index0 != -1) {
				uri = uri.substring(index0 + 1, uri.indexOf(">", index0));
			}

			// Extract a Tel-URI
			int index1 = uri.indexOf(TEL_URI_HEADER);
			if (index1 != -1) {
				uri = uri.substring(index1 + 4);
			}

			// Extract a SIP-URI
			index1 = uri.indexOf(SIP_URI_HEADER);
			if (index1 != -1) {
				int index2 = uri.indexOf("@", index1);
				uri = uri.substring(index1 + 4, index2);
			}

			// Remove URI parameters
			int index2 = uri.indexOf(";");
			if (index2 != -1) {
				uri = uri.substring(0, index2);
			}

			// Returns the extracted number (username part of the URI)
			return uri;
			
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Extract user part phone number from a SIP-URI or Tel-URI or SIP address
	 * 
	 * @param uri SIP or Tel URI
	 * @return Number or null in case of error
	 */
	public static String extractNumberFromUri(String uri) {
		// Format the extracted number (username part of the URI)
		try {
			ContactId contact = ContactUtils.createContactId(extractNumberFromUriWithoutFormatting(uri));
			return contact.toString();
			
		} catch (RcsContactFormatException e) {
			return null;
		}
	}
		
	/**
	 * get URI from SIP identity header
	 * 
	 * @param header
	 *            the SIP header
	 * @return the Uri
	 */
	public static String extractUriFromSipHeader(String header) {
		if (header != null) {
			Matcher matcher = PATTERN_EXTRACT_URI.matcher(header);
			if (matcher.find()) {
				return matcher.group(1);
				
			}
		}
		return header;
	}

}
