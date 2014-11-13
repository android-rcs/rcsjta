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
package com.orangelabs.rcs.ri.utils;

import android.content.Context;
import android.util.Log;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.contacts.ContactsService;
import com.gsma.services.rcs.contacts.RcsContact;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.R;

/**
 * Utilities to manage the RCS display name
 * 
 * @author YPLO6403
 *
 */
public class RcsDisplayName {

	private static final String LOGTAG = LogUtils.getTag(RcsDisplayName.class.getSimpleName());

	/**
	 * Singleton of RcsDisplayName
	 */
	private static volatile RcsDisplayName mInstance;

	private Context mContext;
	private ContactsService mService;
	private ContactUtils mContactUtils;
	
	/**
	 * The default display name
	 */
	private static String DefaultDisplayName; 

	/**
	 * Constructor
	 * @param context
	 */
	private RcsDisplayName(Context context) {
		mContext = context;
		mService = ApiConnectionManager.getInstance(mContext).getContactsApi();
		mContactUtils = ContactUtils.getInstance(mContext);
		DefaultDisplayName = context.getString(R.string.label_no_contact);
	}

	/**
	 * Get an instance of RcsDisplayName.
	 * 
	 * @param context
	 *            the context
	 * @return the singleton instance.
	 */
	public static RcsDisplayName getInstance(Context context) {
		if (mInstance == null) {
			synchronized (RcsDisplayName.class) {
				if (mInstance == null) {
					if (context == null) {
						throw new IllegalArgumentException("Context is null");
					}
					mInstance = new RcsDisplayName(context);
				}
			}
		}
		return mInstance;
	}

	/**
	 * Returns RCS display name which can be displayed on UI
	 * 
	 * @param contact
	 * @return the RCS display name
	 */
	public String getDisplayName(ContactId contact) {
		if (contact == null) {
			return DefaultDisplayName;
		}
		try {
			if (mService == null) {
				mService = ApiConnectionManager.getInstance(mContext).getContactsApi();
			}
			RcsContact rcsContact = mService.getRcsContact(contact);
			String displayName = rcsContact.getDisplayName();
			if (displayName == null) {
				return contact.toString();
			} else {
				return displayName;
			}
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot get displayName", e);
			}
			return contact.toString();
		}
	}

	/**
	 * get RCS display in a String which can be displayed on UI
	 * 
	 * @param number
	 * @return the name which can be displayed on UI
	 */
	public String getDisplayName(String number) {
		if (number == null) {
			return DefaultDisplayName;
		}
		try {
			ContactId contact = mContactUtils.formatContact(number);
			return getDisplayName(contact);
		} catch (RcsContactFormatException e) {
			return number;
		}
	}
}