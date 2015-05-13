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

import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.contact.RcsContact;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.R;

/**
 * Contact list adapter
 */
public class ContactListAdapter extends CursorAdapter {

    private static String[] PROJECTION_PHONE = new String[] {
            Phone._ID, Phone.NUMBER, Phone.LABEL, Phone.TYPE, Phone.CONTACT_ID
    };

    private static String[] PROJECTION_CONTACT = new String[] {
        Contacts.DISPLAY_NAME
    };

    private static String WHERE_CLAUSE_PHONE = new StringBuilder(Phone.NUMBER).append("!='null'")
            .toString();

    private static String WHERE_CLAUSE_CONTACT = new StringBuilder(Contacts._ID).append("=?")
            .toString();

    private static final String LOGTAG = LogUtils.getTag(ContactListAdapter.class.getSimpleName());

    /**
     * Create a contact selector based on the native address book
     * 
     * @param context
     * @return List adapter
     */
    public static ContactListAdapter createContactListAdapter(Context context) {
        return createContactListAdapter(context, null);
    }

    /**
     * Create a contact selector based on the native address book. This selector adds a default
     * value to allow no contact selection
     * 
     * @param context
     * @param defaultValue
     * @return List adapter
     */
    public static ContactListAdapter createContactListAdapter(Context context, String defaultValue) {
        ContentResolver content = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = content.query(Phone.CONTENT_URI, PROJECTION_PHONE, WHERE_CLAUSE_PHONE, null,
                    null);
            Set<ContactId> treatedNumbers = new HashSet<ContactId>();
            MatrixCursor matrix = new MatrixCursor(PROJECTION_PHONE);
            if (defaultValue != null) {
                matrix.addRow(new Object[] {
                        -1, defaultValue, "", -1, -1
                });
            }
            int columnIdxId = cursor.getColumnIndexOrThrow(Phone._ID);
            int columIdxLabel = cursor.getColumnIndexOrThrow(Phone.LABEL);
            int columnIdxType = cursor.getColumnIndexOrThrow(Phone.TYPE);
            int columnIdxContactId = cursor.getColumnIndexOrThrow(Phone.CONTACT_ID);
            int columnIdxNumber = cursor.getColumnIndexOrThrow(Phone.NUMBER);
            while (cursor.moveToNext()) {
                String phoneNumber = cursor.getString(columnIdxNumber);
                if (!ContactUtil.isValidContact(phoneNumber)) {
                    /* Not a valid phone number: skip it */
                    continue;
                }
                ContactId contact = ContactUtil.formatContact(phoneNumber);
                /* Filter number already treated */
                if (!treatedNumbers.contains(contact)) {
                    matrix.addRow(new Object[] {
                            cursor.getLong(columnIdxId), contact.toString(),
                            cursor.getString(columIdxLabel), cursor.getInt(columnIdxType),
                            cursor.getLong(columnIdxContactId)
                    });
                    treatedNumbers.add(contact);
                }
            }
            return new ContactListAdapter(context, matrix);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Create a contact selector with RCS capable contacts
     * 
     * @param context
     * @return List adapter
     */
    public static ContactListAdapter createRcsContactListAdapter(Context context) {
        ContentResolver content = context.getContentResolver();
        Cursor cursor = null;
        ConnectionManager apiConnectionManager = ConnectionManager.getInstance(context);
        MatrixCursor matrix = new MatrixCursor(PROJECTION_PHONE);
        ContactService contactsApi = apiConnectionManager.getContactApi();
        try {
            // Get the set of RCS contacts
            Set<RcsContact> rcsContacts = contactsApi.getRcsContacts();
            // Is there any RCS contacts ?
            if (rcsContacts != null && !rcsContacts.isEmpty()) {
                Set<ContactId> rcsContactIds = new HashSet<ContactId>();
                for (RcsContact rcsContact : rcsContacts) {
                    rcsContactIds.add(rcsContact.getContactId());
                }
                // Query all phone numbers
                cursor = content.query(Phone.CONTENT_URI, PROJECTION_PHONE, WHERE_CLAUSE_PHONE,
                        null, null);
                Set<ContactId> treatedContactIDs = new HashSet<ContactId>();
                int columnIdxId = cursor.getColumnIndexOrThrow(Phone._ID);
                int columIdxLabel = cursor.getColumnIndexOrThrow(Phone.LABEL);
                int columnIdxType = cursor.getColumnIndexOrThrow(Phone.TYPE);
                int columnIdxContactId = cursor.getColumnIndexOrThrow(Phone.CONTACT_ID);
                int columnIdxNumber = cursor.getColumnIndexOrThrow(Phone.NUMBER);
                while (cursor.moveToNext()) {
                    // Keep a trace of already treated row
                    String phoneNumber = cursor.getString(columnIdxNumber);
                    if (!ContactUtil.isValidContact(phoneNumber)) {
                        /* Not a valid phone number: skip it */
                        continue;
                    }
                    ContactId contact = ContactUtil.formatContact(phoneNumber);
                    // If this number is RCS and not already in the list,
                    // take it
                    if (rcsContactIds.contains(contact) && !treatedContactIDs.contains(contact)) {
                        matrix.addRow(new Object[] {
                                cursor.getLong(columnIdxId), contact.toString(),
                                cursor.getString(columIdxLabel), cursor.getInt(columnIdxType),
                                cursor.getLong(columnIdxContactId)
                        });
                        treatedContactIDs.add(contact);
                    }
                }
            }
            return new ContactListAdapter(context, matrix);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "createRcsContactListAdapter", e);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Constructor
     * 
     * @param context Context
     * @param c Cursor
     */
    private ContactListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        TextView view = (TextView) inflater.inflate(R.layout.utils_spinner_item, parent, false);
        view.setTag(new ViewHolder(view, cursor));
        return view;
    }

    @Override
    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        TextView view = (TextView) inflater.inflate(android.R.layout.simple_dropdown_item_1line,
                parent, false);
        view.setTag(new ViewHolder(view, cursor));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        ((TextView) view).setText(formatText(context, cursor, holder));
        // Put the number in tag so it can be retrieved easily
        holder.number = cursor.getString(holder.columnNumber);
    }

    /**
     * Format the item to be displayed. The user name + label is displayed if not null, else the
     * phone number is used
     * 
     * @param context Context
     * @param c Cursor
     * @param holder the holder of column indexes
     * @return the formated text
     */
    private String formatText(Context context, Cursor c, ViewHolder holder) {
        // Get phone label
        String label = c.getString(holder.columnLabel);
        if (label == null) {
            // Label is not custom, get the string corresponding to the phone
            // type
            int type = c.getInt(holder.columnType);
            label = context.getString(Phone.getTypeLabelResource(type));
        }

        String name = null;
        String[] selectionArgs = new String[] {
            Long.toString(c.getLong(holder.columnContactId))
        };
        // Get contact name from contact id
        Cursor personCursor = context.getContentResolver().query(Contacts.CONTENT_URI,
                PROJECTION_CONTACT, WHERE_CLAUSE_CONTACT, selectionArgs, null);
        if (personCursor.moveToFirst()) {
            name = personCursor.getString(holder.columnID);
        }
        personCursor.close();
        if (name == null) {
            // Return "phone number"
            return c.getString(holder.columnNumber);
        } else {
            // Return "name (phone label)"
            return new StringBuilder(name).append(" (").append(label).append(")").toString();
        }
    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * getColumnIndex() on each row.
     */
    private class ViewHolder {
        int columnID;

        int columnNumber;

        int columnLabel;

        int columnType;

        int columnContactId;

        // Store number to get it upon selection
        String number;

        ViewHolder(View base, Cursor cursor) {
            columnID = cursor.getColumnIndexOrThrow(Phone._ID);
            columnNumber = cursor.getColumnIndexOrThrow(Phone.NUMBER);
            columnLabel = cursor.getColumnIndexOrThrow(Phone.LABEL);
            columnType = cursor.getColumnIndexOrThrow(Phone.TYPE);
            columnContactId = cursor.getColumnIndexOrThrow(Phone.CONTACT_ID);
            number = cursor.getString(columnNumber);
        }
    }

    /**
     * Get the selected phone number
     * 
     * @param view
     * @return the phone number
     */
    public String getSelectedNumber(View view) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        return holder.number;
    }
}
