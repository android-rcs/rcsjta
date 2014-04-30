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

import com.orangelabs.rcs.platform.AndroidFactory;

/**
 * Contacts utility functions
 */
public class ContactUtils {
	/**
	 * Returns the contact id associated to a contact number in the Address Book
	 * 
	 * @parma context Application context
	 * @param number Contact number
	 * @return Id or -1 if the contact number does not exist
	 */
	public static int getContactId(Context context, String number) {
		int id = -1;
		
		// Format number
		number = PhoneUtils.extractNumberFromUri(number);
		
		// Query the Phone API
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        		new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,  ContactsContract.CommonDataKinds.Phone.NUMBER},
        		null, 
     		    null, 
     		    null);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				String databaseNumber = PhoneUtils.extractNumberFromUri(cursor.getString(1));
				if (databaseNumber.equals(number)) {
					id = cursor.getInt(0);
					break;
				}
			}
			cursor.close();
		}       	
        return id;
	}
	
	/**
	 * Create a RCS contact if the given contact is not already present in the address book
	 * 
	 * @param context Application context
	 * @param contact Contact number
	 * @return URI of the newly created contact or URI of the corresponding contact if there is already a match
	 */
	public static Uri createRcsContactIfNeeded(Context context, String number) throws Exception{
		// Check if contact is already in address book
		int contactId = getContactId(context, number);
		
		if (contactId==-1){
			// If the contact is not present in address book, create an entry with this number
			ContentValues values = new ContentValues();

			values.putNull(ContactsContract.Contacts.DISPLAY_NAME);
			values.put(Phone.NUMBER, number);
			values.put(Phone.TYPE, Phone.TYPE_MOBILE);
			
			Uri newPersonUri = createContact(context, values);
			
			return newPersonUri;
		} else {
			// Contact already in address book
			return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
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
	public static Uri createContact(Context context, ContentValues values) throws Exception{
		ContentResolver mResolver = context.getContentResolver();
		
		// We will associate the newly created contact to the null contact account (Phone)
		
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        
		int backRefIndex = 0;
		operations.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
					.withValue(RawContacts.ACCOUNT_TYPE, null)
					.withValue(RawContacts.ACCOUNT_NAME, null)
					.build());   

		// Set the name
        operations.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        		.withValueBackReference(Data.RAW_CONTACT_ID, backRefIndex)
        		.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        		.withValue(StructuredName.DISPLAY_NAME, values.get(ContactsContract.Contacts.DISPLAY_NAME))
        		.build());
        
        operations.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
        		.withValueBackReference(Data.RAW_CONTACT_ID, backRefIndex)
        		.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
        		.withValue(Phone.NUMBER, values.get(Phone.NUMBER))
        		.withValue(Phone.TYPE, values.get(Phone.TYPE))
        		.build());
        
        long rawContactId = 0;
		ContentProviderResult[] result = mResolver.applyBatch(ContactsContract.AUTHORITY, operations);
		rawContactId = ContentUris.parseId(result[1].uri);
		long contactId = 0;
		// Search the corresponding contact id
		Cursor c = mResolver.query(Data.CONTENT_URI,          
				new String[]{Data.CONTACT_ID},          
				Data._ID + "=?",          
				new String[] {String.valueOf(rawContactId)}, 
				null);
		if (c != null) {
			if (c.moveToFirst()) {
				contactId = c.getLong(0);
			}
			c.close();
		}		
		// Return the resulting contact uri
		Uri resultUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId); 
		return resultUri;
	}

	/**
	 * Check if the given number is present in the address book
	 * 
	 * @param number Number to be checked
	 * @return boolean indicating if number is present in the address book or not
	 */
	public static boolean isNumberInAddressBook(String number){
		String[] projection = { Data.RAW_CONTACT_ID };
        String selection = Data.MIMETYPE + "=? AND PHONE_NUMBERS_EQUAL(" + Phone.NUMBER + ", ?)";
        String[] selectionArgs = { Phone.CONTENT_ITEM_TYPE, number };
	    String sortOrder = Data.RAW_CONTACT_ID;

	    ContentResolver contentResolver = AndroidFactory.getApplicationContext().getContentResolver();
	    
	    // Starting query phone_numbers_equal
	    Cursor cur = contentResolver.query(Data.CONTENT_URI, 
	    		projection, 
	    		selection, 
	    		selectionArgs,
	    		sortOrder);
	    
	    if (cur != null) {
	    	int count = cur.getCount();
	    	cur.close();
	    	// We found at least one data with this number
	    	if (count>0){
	    		return true;
	    	}
	    }

	    // No match found using LOOSE equals, try using STRICT equals.
	    String selectionStrict = Data.MIMETYPE + "=? AND (NOT PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
	    + ", ?) AND PHONE_NUMBERS_EQUAL(" + Phone.NUMBER + ", ?, 1))";
	    String[] selectionArgsStrict = {
	    		Phone.CONTENT_ITEM_TYPE, number, number
	    };
	    cur = contentResolver.query(Data.CONTENT_URI, 
	    		projection, 
	    		selectionStrict, 
	    		selectionArgsStrict,
	    		sortOrder);
	    if (cur != null) {
	    	int count = cur.getCount();
	    	cur.close();
	    	// We found at least one data with this number
	    	if (count>0){
	    		return true;
	    	}
	    }
	    
	    // We found no contact with this number
	    return false;
	}	
}
