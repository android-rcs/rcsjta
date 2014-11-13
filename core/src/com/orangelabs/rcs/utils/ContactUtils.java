/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.utils;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.platform.AndroidFactory;

/**
 * Contacts utility functions
 */
public class ContactUtils {

	private static final String[] PROJECTION_CONTACTID_NUMBER = new String[] { ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
			ContactsContract.CommonDataKinds.Phone.NUMBER };

	private static final String[] PROJECTION_RAW_CONTACT_ID = new String[] { Data.RAW_CONTACT_ID };

	private static final String SELECTION_LOOSE = new StringBuilder(Data.MIMETYPE).append("=? AND PHONE_NUMBERS_EQUAL(")
			.append(Phone.NUMBER).append(", ?)").toString();

	private static final String SELECTION_STRICT = new StringBuilder(Data.MIMETYPE).append("=? AND (NOT PHONE_NUMBERS_EQUAL(")
			.append(Phone.NUMBER).append(", ?) AND PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER).append(", ?, 1))").toString();

	private static final String[] PROJECTION_DATA_CONTACTID = new String[] { Data.CONTACT_ID };
	
	private static final String WHERE_DATA_ID = new StringBuilder(Data._ID).append("=?").toString();
	
	/**
	 * Returns the contact id associated to a contact number in the Address Book
	 * 
	 * @param context Application context
	 * @param contactId Contact ID
	 * @return Id or -1 if the contact number does not exist
	 */
	private static int getContactIdOfAddressBook(Context context, ContactId contactId) {
		// Query the Phone API
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
					PROJECTION_CONTACTID_NUMBER, null, null, null);
			while (cursor.moveToNext()) {
				String databaseNumber = PhoneUtils.extractNumberFromUri(cursor.getString(1));
				if (contactId.toString().equals(databaseNumber)) {
					return cursor.getInt(0);
				}
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return -1;
	}
	
	/**
	 * Create a RCS contact if the given contact is not already present in the address book
	 * 
	 * @param context Application context
	 * @param contactId Contact ID
	 * @return URI of the newly created contact or URI of the corresponding contact if there is already a match
	 */
	public static Uri createRcsContactIfNeeded(Context context, ContactId contactId) throws Exception{
		// Check if contact is already in address book
		int phoneId = getContactIdOfAddressBook(context, contactId);
		
		if (phoneId == -1) {
			// If the contact is not present in address book, create an entry with this number
			ContentValues values = new ContentValues();

			values.putNull(ContactsContract.Contacts.DISPLAY_NAME);
			values.put(Phone.NUMBER, contactId.toString());
			values.put(Phone.TYPE, Phone.TYPE_MOBILE);
			
			Uri newPersonUri = createContact(context, values);
			
			return newPersonUri;
		} else {
			// Contact already in address book
			return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, phoneId);
		}
	}

	/**
	 * Create a contact in address book
	 * <br>This is done with Contacts 2.0 API, and new contact is a "Phone" contact, not associated with any particular account type
	 * 
	 * @param context Application context
	 * @param values Contact values
	 * @return URI of the created contact
	 */
	private static Uri createContact(Context context, ContentValues values) throws Exception {
		ContentResolver mResolver = context.getContentResolver();

		// We will associate the newly created contact to the null contact account (Phone)

		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

		int backRefIndex = 0;
		operations.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValue(RawContacts.ACCOUNT_TYPE, null)
				.withValue(RawContacts.ACCOUNT_NAME, null).build());

		// Set the name
		operations.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, backRefIndex)
				.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
				.withValue(StructuredName.DISPLAY_NAME, values.get(ContactsContract.Contacts.DISPLAY_NAME)).build());

		operations.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, backRefIndex).withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER, values.get(Phone.NUMBER)).withValue(Phone.TYPE, values.get(Phone.TYPE)).build());

		long rawContactId = 0;
		ContentProviderResult[] result = mResolver.applyBatch(ContactsContract.AUTHORITY, operations);
		rawContactId = ContentUris.parseId(result[1].uri);
		long contactId = 0;
		// Search the corresponding contact id
		Cursor c = null;
		try {
			String[] whereArgs = new String[] { String.valueOf(rawContactId) };
			c = mResolver.query(Data.CONTENT_URI, PROJECTION_DATA_CONTACTID, WHERE_DATA_ID, whereArgs, null);
			if (c.moveToFirst()) {
				contactId = c.getLong(0);
			}
		} catch (Exception e) {
		} finally {
			if (c != null) {
				c.close();
			}
		}
		// Return the resulting contact uri
		return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
	}

	/**
	 * Check if the given number is present in the address book
	 * 
	 * @param contact Contact ID to be checked
	 * @return boolean indicating if number is present in the address book or not
	 */
	public static boolean isNumberInAddressBook(ContactId contact){
		String[] selectionArgs = { Phone.CONTENT_ITEM_TYPE, contact.toString() };
		ContentResolver contentResolver = AndroidFactory.getApplicationContext().getContentResolver();
		
		// Starting query phone_numbers_equal
		Cursor cur = null;
		try {
			cur = contentResolver.query(Data.CONTENT_URI, PROJECTION_RAW_CONTACT_ID, SELECTION_LOOSE, selectionArgs, Data.RAW_CONTACT_ID);
			// We found at least one data with this number
			if (cur.getCount() > 0) {
				return true;
			}
		} catch (Exception e) {
		} finally {
			if (cur != null) {
				cur.close();
				cur = null;
			}
		}

		// No match found using LOOSE equals, try using STRICT equals.
		String[] selectionArgsStrict = { Phone.CONTENT_ITEM_TYPE, contact.toString(), contact.toString() };
		try {
			cur = contentResolver.query(Data.CONTENT_URI, PROJECTION_RAW_CONTACT_ID, SELECTION_STRICT, selectionArgsStrict,
					Data.RAW_CONTACT_ID);
			// We found at least one data with this number
			if (cur.getCount() > 0) {
				return true;
			}
		} catch (Exception e) {
		} finally {
			if (cur != null) {
				cur.close();
			}
		}

		// We found no contact with this number
		return false;
	}
	
	/**
	 * Create a ContactId from a phone number received in the payload
	 * 
	 * @param phoneNumber
	 *            the phone number
	 * @return the Contact Identifier
	 * @throws RcsContactFormatException
	 */
	public static ContactId createContactId(String phoneNumber) throws RcsContactFormatException {
		com.gsma.services.rcs.contacts.ContactUtils contactUtils = com.gsma.services.rcs.contacts.ContactUtils
				.getInstance(AndroidFactory.getApplicationContext());
		if (contactUtils != null) {
			return contactUtils.formatContact(PhoneUtils.extractNumberFromUriWithoutFormatting(phoneNumber));
		}
		throw new RcsContactFormatException();
	}
}
