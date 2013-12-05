/*
 * Copyright (C) 2009 The Android Open Source Project
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
 */

package com.gsma.services.rcs.samples.contacts;


import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactsUtils {
    private static final String TAG = "ContactsUtils";
    private static final String WAIT_SYMBOL_AS_STRING = String.valueOf(PhoneNumberUtils.WAIT);
       
    
    /**
     * Opens an InputStream for the person's photo and returns the photo as a Bitmap.
     * If the person's photo isn't present returns null.
     *
     * @param aggCursor the Cursor pointing to the data record containing the photo.
     * @param bitmapColumnIndex the column index where the photo Uri is stored.
     * @param options the decoding options, can be set to null
     * @return the photo Bitmap
     */
    public static Bitmap loadContactPhoto(Cursor cursor, int bitmapColumnIndex,
            BitmapFactory.Options options) {
        if (cursor == null) {
            return null;
        }

        byte[] data = cursor.getBlob(bitmapColumnIndex);
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    /**
     * Loads a placeholder photo.
     *
     * @param placeholderImageResource the resource to use for the placeholder image
     * @param context the Context
     * @param options the decoding options, can be set to null
     * @return the placeholder Bitmap.
     */
    public static Bitmap loadPlaceholderPhoto(int placeholderImageResource, Context context,
            BitmapFactory.Options options) {
        if (placeholderImageResource == 0) {
            return null;
        }
        return BitmapFactory.decodeResource(context.getResources(),
                placeholderImageResource, options);
    }

    public static Bitmap loadContactPhoto(Context context, long photoId,
            BitmapFactory.Options options) {
        Cursor photoCursor = null;
        Bitmap photoBm = null;

        try {
            photoCursor = context.getContentResolver().query(
                    ContentUris.withAppendedId(Data.CONTENT_URI, photoId),
                    new String[] { Photo.PHOTO },
                    null, null, null);

            if (photoCursor.moveToFirst() && !photoCursor.isNull(0)) {
                byte[] photoData = photoCursor.getBlob(0);
                photoBm = BitmapFactory.decodeByteArray(photoData, 0,
                        photoData.length, options);
            }
        } finally {
            if (photoCursor != null) {
                photoCursor.close();
            }
        }

        return photoBm;
    }

    // TODO find a proper place for the canonical version of these
    public interface ProviderNames {
        String YAHOO = "Yahoo";
        String GTALK = "GTalk";
        String MSN = "MSN";
        String ICQ = "ICQ";
        String AIM = "AIM";
        String XMPP = "XMPP";
        String JABBER = "JABBER";
        String SKYPE = "SKYPE";
        String QQ = "QQ";
    }

    /**
     * This looks up the provider name defined in
     * ProviderNames from the predefined IM protocol id.
     * This is used for interacting with the IM application.
     *
     * @param protocol the protocol ID
     * @return the provider name the IM app uses for the given protocol, or null if no
     * provider is defined for the given protocol
     * @hide
     */
    public static String lookupProviderNameFromId(int protocol) {
        switch (protocol) {
            case Im.PROTOCOL_GOOGLE_TALK:
                return ProviderNames.GTALK;
            case Im.PROTOCOL_AIM:
                return ProviderNames.AIM;
            case Im.PROTOCOL_MSN:
                return ProviderNames.MSN;
            case Im.PROTOCOL_YAHOO:
                return ProviderNames.YAHOO;
            case Im.PROTOCOL_ICQ:
                return ProviderNames.ICQ;
            case Im.PROTOCOL_JABBER:
                return ProviderNames.JABBER;
            case Im.PROTOCOL_SKYPE:
                return ProviderNames.SKYPE;
            case Im.PROTOCOL_QQ:
                return ProviderNames.QQ;
        }
        return null;
    }


    private static boolean isProtocolValid(ContentValues values) {
        String protocolString = values.getAsString(Im.PROTOCOL);
        if (protocolString == null) {
            return false;
        }
        try {
            Integer.valueOf(protocolString);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static long queryForContactId(ContentResolver cr, long rawContactId) {
        Cursor contactIdCursor = null;
        long contactId = -1;
        try {
            contactIdCursor = cr.query(RawContacts.CONTENT_URI,
                    new String[] {RawContacts.CONTACT_ID},
                    RawContacts._ID + "=" + rawContactId, null, null);
            if (contactIdCursor != null && contactIdCursor.moveToFirst()) {
                contactId = contactIdCursor.getLong(0);
            }
        } finally {
            if (contactIdCursor != null) {
                contactIdCursor.close();
            }
        }
        return contactId;
    }

    public static String querySuperPrimaryPhone(ContentResolver cr, long contactId) {
        Cursor c = null;
        String phone = null;
        try {
            Uri baseUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
            Uri dataUri = Uri.withAppendedPath(baseUri, Contacts.Data.CONTENT_DIRECTORY);

            c = cr.query(dataUri,
                    new String[] {Phone.NUMBER},
                    Data.MIMETYPE + "=" + Phone.MIMETYPE +
                        " AND " + Data.IS_SUPER_PRIMARY + "=1",
                    null, null);
            if (c != null && c.moveToFirst()) {
                // Just return the first one.
                phone = c.getString(0);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return phone;
    }

    public static long queryForRawContactId(ContentResolver cr, long contactId) {
        Cursor rawContactIdCursor = null;
        long rawContactId = -1;
        try {
            rawContactIdCursor = cr.query(RawContacts.CONTENT_URI,
                    new String[] {RawContacts._ID},
                    RawContacts.CONTACT_ID + "=" + contactId, null, null);
            if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
                // Just return the first one.
                rawContactId = rawContactIdCursor.getLong(0);
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }
        return rawContactId;
    }

    public static ArrayList<Long> queryForAllRawContactIds(ContentResolver cr, long contactId) {
        Cursor rawContactIdCursor = null;
        ArrayList<Long> rawContactIds = new ArrayList<Long>();
        try {
            rawContactIdCursor = cr.query(RawContacts.CONTENT_URI,
                    new String[] {RawContacts._ID},
                    RawContacts.CONTACT_ID + "=" + contactId, null, null);
            if (rawContactIdCursor != null) {
                while (rawContactIdCursor.moveToNext()) {
                    rawContactIds.add(rawContactIdCursor.getLong(0));
                }
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }
        return rawContactIds;
    }


    /**
     * Utility for creating a standard tab indicator view.
     *
     * @param parent The parent ViewGroup to attach the new view to.
     * @param label The label to display in the tab indicator. If null, not label will be displayed.
     * @param icon The icon to display. If null, no icon will be displayed.
     * @return The tab indicator View.
     */
    public static View createTabIndicatorView(ViewGroup parent, CharSequence label, Drawable icon) {
        final LayoutInflater inflater = (LayoutInflater)parent.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View tabIndicator = inflater.inflate(R.layout.tab_indicator, parent, false);
        tabIndicator.getBackground().setDither(true);

        final TextView tv = (TextView) tabIndicator.findViewById(R.id.tab_title);
        tv.setText(label);

        final ImageView iconView = (ImageView) tabIndicator.findViewById(R.id.tab_icon);
        iconView.setImageDrawable(icon);

        return tabIndicator;
    }

   

    /**
     * Kick off an intent to initiate a call.
     *
     * @param phoneNumber must not be null.
     * @throws NullPointerException when the given argument is null.
     */
    public static void initiateCall(Context context, CharSequence phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL,
                Uri.fromParts("tel", phoneNumber.toString(), null));
        context.startActivity(intent);
    }

    /**
     * Kick off an intent to initiate an Sms/Mms message.
     *
     * @param phoneNumber must not be null.
     * @throws NullPointerException when the given argument is null.
     */
    public static void initiateSms(Context context, CharSequence phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("sms", phoneNumber.toString(), null));
        context.startActivity(intent);
    }

    /**
     * Test if the given {@link CharSequence} contains any graphic characters,
     * first checking {@link TextUtils#isEmpty(CharSequence)} to handle null.
     */
    public static boolean isGraphic(CharSequence str) {
        return !TextUtils.isEmpty(str) && TextUtils.isGraphic(str);
    }

    /**
     * Returns true if two objects are considered equal.  Two null references are equal here.
     */
    public static boolean areObjectsEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Returns true if two data with mimetypes which represent values in contact entries are
     * considered equal for collapsing in the GUI. For caller-id, use
     * {@link PhoneNumberUtils#compare(Context, String, String)} instead
     */
    public static final boolean shouldCollapse(Context context, CharSequence mimetype1,
            CharSequence data1, CharSequence mimetype2, CharSequence data2) {
        if (TextUtils.equals(Phone.CONTENT_ITEM_TYPE, mimetype1)
                && TextUtils.equals(Phone.CONTENT_ITEM_TYPE, mimetype2)) {
            if (data1 == data2) {
                return true;
            }
            if (data1 == null || data2 == null) {
                return false;
            }

            // If the number contains semicolons, PhoneNumberUtils.compare
            // only checks the substring before that (which is fine for caller-id usually)
            // but not for collapsing numbers. so we check each segment indidually to be more strict
            // TODO: This should be replaced once we have a more robust phonenumber-library
            String[] dataParts1 = data1.toString().split(WAIT_SYMBOL_AS_STRING);
            String[] dataParts2 = data2.toString().split(WAIT_SYMBOL_AS_STRING);
            if (dataParts1.length != dataParts2.length) {
                return false;
            }
            for (int i = 0; i < dataParts1.length; i++) {
                if (!PhoneNumberUtils.compare(context, dataParts1[i], dataParts2[i])) {
                    return false;
                }
            }

            return true;
        } else {
            if (mimetype1 == mimetype2 && data1 == data2) {
                return true;
            }
            return TextUtils.equals(mimetype1, mimetype2) && TextUtils.equals(data1, data2);
        }
    }

    /**
     * Returns true if two {@link Intent}s are both null, or have the same action.
     */
    public static final boolean areIntentActionEqual(Intent a, Intent b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return TextUtils.equals(a.getAction(), b.getAction());
    }
}
