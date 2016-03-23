/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.presence;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

import java.util.ArrayList;

/**
 * Presence utility functions
 * 
 * @author jexa7410
 */
public class PresenceUtils {
    /**
     * RCS 2.0 video share feature tag
     */
    public final static String FEATURE_RCS2_VIDEO_SHARE = "org.gsma.videoshare";

    /**
     * RCS 2.0 image share feature tag
     */
    public final static String FEATURE_RCS2_IMAGE_SHARE = "org.gsma.imageshare";

    /**
     * RCS 2.0 file transfer feature tag
     */
    public final static String FEATURE_RCS2_FT = "org.openmobilealliance:File-Transfer";

    /**
     * RCS 2.0 chat feature tag
     */
    public final static String FEATURE_RCS2_CHAT = "org.openmobilealliance:IM-session";

    /**
     * RCS 2.0 CS video feature tag
     */
    public final static String FEATURE_RCS2_CS_VIDEO = "org.3gpp.cs-videotelephony";

    private static final String[] PROJECTION_CONTACTID_NUMBER = new String[] {
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };

    private static final String[] PROJECTION_RAW_CONTACT_ID = new String[] {
        Data.RAW_CONTACT_ID
    };

    private static final String SELECTION_LOOSE = new StringBuilder(Data.MIMETYPE)
            .append("=? AND PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER).append(", ?)").toString();

    private static final String SELECTION_STRICT = new StringBuilder(Data.MIMETYPE)
            .append("=? AND (NOT PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER)
            .append(", ?) AND PHONE_NUMBERS_EQUAL(").append(Phone.NUMBER).append(", ?, 1))")
            .toString();

    private static final String[] PROJECTION_DATA_CONTACTID = new String[] {
        Data.CONTACT_ID
    };

    private static final String WHERE_DATA_ID = new StringBuilder(Data._ID).append("=?").toString();

    private static int INVALID_CONTACT_ID = -1;

    /**
     * Returns the contact id associated to a contact number in the Address Book
     * 
     * @param context Application context
     * @param contact Contact ID
     * @return Id or -1 if the contact number does not exist
     */
    private static int getContactIdOfAddressBook(Context context, ContactId contact) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    PROJECTION_CONTACTID_NUMBER, null, null, null);
            CursorUtil.assertCursorIsNotNull(cursor,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            if (!cursor.moveToNext()) {
                return INVALID_CONTACT_ID;
            }
            int columnIdxContactId = cursor
                    .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int columnIdxNumber = cursor
                    .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
            do {
                PhoneNumber number = ContactUtil.getValidPhoneNumberFromAndroid(cursor
                        .getString(columnIdxNumber));
                if (number == null) {
                    continue;
                }
                ContactId contactInAddressBook = ContactUtil
                        .createContactIdFromValidatedData(number);
                if (contactInAddressBook.equals(contact)) {
                    return cursor.getInt(columnIdxContactId);
                }
            } while (cursor.moveToNext());

        } finally {
            CursorUtil.close(cursor);
        }
        return INVALID_CONTACT_ID;
    }

    /**
     * Create a contact in address book <br>
     * This is done with Contacts 2.0 API, and new contact is a "Phone" contact, not associated with
     * any particular account type
     * 
     * @param context Application context
     * @param values Contact values
     * @return URI of the created contact
     * @throws OperationApplicationException
     * @throws RemoteException
     */
    private static Uri createContact(Context context, ContentValues values) throws RemoteException,
            OperationApplicationException {
        ContentResolver mResolver = context.getContentResolver();
        /* We will associate the newly created contact to the null contact account (Phone) */
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        int backRefIndex = 0;
        operations.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null).build());

        /* Set the name */
        operations.add(ContentProviderOperation
                .newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, backRefIndex)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME,
                        values.get(ContactsContract.Contacts.DISPLAY_NAME)).build());

        operations.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, backRefIndex)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, values.get(Phone.NUMBER))
                .withValue(Phone.TYPE, values.get(Phone.TYPE)).build());

        long rawContactId = 0;
        ContentProviderResult[] result = mResolver.applyBatch(ContactsContract.AUTHORITY,
                operations);
        rawContactId = ContentUris.parseId(result[1].uri);
        long contactId = 0;
        /* Search the corresponding contact id */
        Cursor c = null;
        String[] whereArgs = new String[] {
            String.valueOf(rawContactId)
        };
        try {
            c = mResolver.query(Data.CONTENT_URI, PROJECTION_DATA_CONTACTID, WHERE_DATA_ID,
                    whereArgs, null);
            CursorUtil.assertCursorIsNotNull(c, Data.CONTENT_URI);
            if (c.moveToFirst()) {
                int columnIdxContactId = c.getColumnIndexOrThrow(Data.CONTACT_ID);
                contactId = c.getLong(columnIdxContactId);
            }

        } finally {
            if (c != null) {
                c.close();
            }
        }
        /* Return the resulting contact uri */
        return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
    }

    /**
     * Create a RCS contact if the given contact is not already present in the address book
     * 
     * @param context Application context
     * @param contactId Contact ID
     * @return URI of the newly created contact or URI of the corresponding contact if there is
     *         already a match
     * @throws OperationApplicationException
     * @throws RemoteException
     */
    /* package private */static Uri createRcsContactIfNeeded(Context context, ContactId contactId)
            throws RemoteException, OperationApplicationException {
        /* Check if contact is already in address book */
        int phoneId = getContactIdOfAddressBook(context, contactId);
        if (phoneId != INVALID_CONTACT_ID) {
            /* Contact already in address book */
            return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, phoneId);
        }
        /* If the contact is not present in address book, create an entry with this number */
        ContentValues values = new ContentValues();
        values.putNull(ContactsContract.Contacts.DISPLAY_NAME);
        values.put(Phone.NUMBER, contactId.toString());
        values.put(Phone.TYPE, Phone.TYPE_MOBILE);
        return createContact(context, values);
    }

    /**
     * Check if the given number is present in the address book
     * 
     * @param contact Contact ID to be checked
     * @return boolean indicating if number is present in the address book or not
     */
    /* package private */static boolean isNumberInAddressBook(ContactId contact) {
        String[] selectionArgs = {
                Phone.CONTENT_ITEM_TYPE, contact.toString()
        };
        ContentResolver contentResolver = AndroidFactory.getApplicationContext()
                .getContentResolver();

        /* Starting query phone_numbers_equal */
        Cursor cur = null;
        try {
            cur = contentResolver.query(Data.CONTENT_URI, PROJECTION_RAW_CONTACT_ID,
                    SELECTION_LOOSE, selectionArgs, Data.RAW_CONTACT_ID);
            CursorUtil.assertCursorIsNotNull(cur, Data.CONTENT_URI);
            /* We found at least one data with this number */
            if (cur.getCount() > 0) {
                return true;
            }
        } finally {
            if (cur != null) {
                cur.close();
                cur = null;
            }
        }

        /* No match found using LOOSE equals, try using STRICT equals. */
        String[] selectionArgsStrict = {
                Phone.CONTENT_ITEM_TYPE, contact.toString(), contact.toString()
        };
        try {
            cur = contentResolver.query(Data.CONTENT_URI, PROJECTION_RAW_CONTACT_ID,
                    SELECTION_STRICT, selectionArgsStrict, Data.RAW_CONTACT_ID);
            CursorUtil.assertCursorIsNotNull(cur, Data.CONTENT_URI);
            /* We found at least one data with this number */
            if (cur.getCount() > 0) {
                return true;
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        /* We found no contact with this number */
        return false;
    }
}
