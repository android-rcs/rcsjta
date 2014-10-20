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

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.contacts.ContactId;
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
	 * Get the RCS display name
	 * 
	 * @param context
	 * @param contact
	 * @return the RCS display name or null
	 */
	public static String get(Context ctx, ContactId contact) {
		try {
			if (contact == null) {
				return null;
			}
			RcsContact rcsContact = ApiConnectionManager.getInstance(ctx).getContactsApi().getRcsContact(contact);
			return rcsContact.getDisplayName();
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot get displayName", e);
			}
		}
		return null;
	}

	/**
	 * Convert RCS display name according to direction and contactId in a String which can be displayed to UI
	 * 
	 * @param context
	 * @param direction
	 * @param contact
	 * @param rcsDisplayName
	 * @return the display name which can be displayed to client
	 */
	public static String convert(Context ctx, int direction, ContactId contact, String rcsDisplayName) {
		if (direction == RcsCommon.Direction.OUTGOING) {
			return ctx.getString(R.string.label_me);
		} else {
			if (rcsDisplayName == null) {
				if (contact != null) {
					return contact.toString();
				} else {
					return ctx.getString(R.string.label_no_contact);
				}
			} else {
				return rcsDisplayName;
			}
		}
	}
}
