/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange
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

package com.gsma.rcs.ri.utils;

import com.gsma.rcs.api.connection.ConnectionManager;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.ri.R;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.contact.RcsContact;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.InputStream;

/**
 * Utilities to manage the RCS display name
 * 
 * @author Philippe LEMORDANT
 */
public class RcsContactUtil {

    private static final int MAX_DISPLAY_NAME_IN_CACHE = 200;
    private static final String LOGTAG = LogUtils.getTag(RcsContactUtil.class.getSimpleName());

    private static volatile RcsContactUtil sInstance;
    private final ContentResolver mResolver;
    private ContactService mService;
    private final String mDefaultDisplayName;
    private LruCache<ContactId, String> mDisplayNameAndroidCache;
    private LruCache<ContactId, Bitmap> mPhotoContactCache;

    private static final String[] PROJ_DISPLAY_NAME = new String[] {
        ContactsContract.PhoneLookup.DISPLAY_NAME
    };

    private static final String[] PROJ_CONTACT_ID = new String[] {
        ContactsContract.PhoneLookup._ID
    };

    /**
     * Constructor
     * 
     * @param context the context
     */
    private RcsContactUtil(Context context) {
        mService = ConnectionManager.getInstance().getContactApi();
        mResolver = context.getContentResolver();
        mDefaultDisplayName = context.getString(R.string.label_no_contact);
        mDisplayNameAndroidCache = new LruCache<>(MAX_DISPLAY_NAME_IN_CACHE);
        mPhotoContactCache = new LruCache<>(MAX_DISPLAY_NAME_IN_CACHE);
    }

    /**
     * Get an instance of RcsDisplayName.
     * 
     * @param context the context
     * @return the singleton instance.
     */
    public static RcsContactUtil getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (RcsContactUtil.class) {
            if (sInstance == null) {
                sInstance = new RcsContactUtil(context);
            }
            return sInstance;
        }
    }

    private String getDisplayNameFromAddressBook(ContactId contact) {
        /* First try to get it from cache */
        String displayName = mDisplayNameAndroidCache.get(contact);
        if (displayName != null) {
            return displayName;
        }
        /* Not found in cache: query the Android address book */
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(contact.toString()));
        Cursor cursor = null;
        try {
            cursor = mResolver.query(uri, PROJ_DISPLAY_NAME, null, null, null);
            if (cursor == null) {
                throw new SQLException("Cannot query display name for contact=" + contact);
            }
            if (!cursor.moveToFirst()) {
                return null;
            }
            displayName = cursor.getString(cursor
                    .getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
            /* Insert in cache */
            mDisplayNameAndroidCache.put(contact, displayName);
            return displayName;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Returns display name which can be displayed on UI
     * 
     * @param contact the contact
     * @return the display name
     */
    public String getDisplayName(ContactId contact) {
        if (contact == null) {
            return mDefaultDisplayName;
        }
        try {
            if (mService == null) {
                mService = ConnectionManager.getInstance().getContactApi();
            }
            RcsContact rcsContact = mService.getRcsContact(contact);
            if (rcsContact == null) {
                String displayName = getDisplayNameFromAddressBook(contact);
                if (displayName != null) {
                    return displayName;
                }
                /*
                 * Contact exists but is not RCS: returns the phone number.
                 */
                return contact.toString();
            }
            String displayName = rcsContact.getDisplayName();
            if (displayName == null) {
                displayName = getDisplayNameFromAddressBook(contact);
                if (displayName != null) {
                    return displayName;
                }
                return contact.toString();
            } else {
                return displayName;
            }
        } catch (RcsServiceNotAvailableException ignore) {
            String displayName = getDisplayNameFromAddressBook(contact);
            if (displayName != null) {
                return displayName;
            }
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
        /* By default display name is set to the MSISDN */
        return contact.toString();
    }

    /**
     * get RCS display in a String which can be displayed on UI
     * 
     * @param number the phone number
     * @return the name which can be displayed on UI
     */
    public String getDisplayName(String number) {
        if (number == null) {
            return mDefaultDisplayName;
        }
        if (!ContactUtil.isValidContact(number)) {
            return number;
        }
        ContactId contact = ContactUtil.formatContact(number);
        return getDisplayName(contact);
    }

    /**
     * Gets the photo of a contact, or null if no photo is present
     * 
     * @param contact the contact ID
     * @return an Bitmap of the photo, or null if no photo is present
     */
    public Bitmap getPhotoFromContactId(ContactId contact) {
        /* First try to get it from cache */
        Bitmap photo = mPhotoContactCache.get(contact);
        if (photo != null) {
            return photo;
        }
        Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI,
                Uri.encode(contact.toString()));
        Cursor cursor = null;
        try {
            cursor = mResolver.query(contactUri, PROJ_CONTACT_ID, null, null, null);
            if (cursor == null) {
                throw new SQLException("Cannot query photo for contact=" + contact);
            }
            if (!cursor.moveToFirst()) {
                return null;
            }
            long contactId = cursor.getLong(cursor
                    .getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
            InputStream photoInputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                    mResolver,
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId));
            if (photoInputStream != null) {
                photo = BitmapFactory.decodeStream(photoInputStream);
                /* Insert in cache */
                mPhotoContactCache.put(contact, photo);
                return photo;
            }
            return null;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
