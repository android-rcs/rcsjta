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

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.contacts.ContactsService;
import com.gsma.services.rcs.contacts.RcsContact;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.R;

/**
 * Contact list adapter
 */
public class ContactListAdapter extends CursorAdapter {

    private static String[] PROJECTION = new String[] {
            Phone._ID, Phone.NUMBER, Phone.LABEL, Phone.TYPE, Phone.CONTACT_ID
    };

    private static String WHERE_CLAUSE = new StringBuilder(Phone.NUMBER).append("!='null'")
            .toString();

    /**
     * The log tag for this class
     */
    private static final String LOGTAG = LogUtils.getTag(ContactListAdapter.class.getSimpleName());

    /**
     * Create a contact selector based on the native address book
     * 
     * @param context
     * @return List adapter
     */
    public static ContactListAdapter createContactListAdapter(Context context) {
        ContentResolver content = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = content.query(Phone.CONTENT_URI, PROJECTION, WHERE_CLAUSE, null, null);
            // Set of unique numbers
            Set<String> treatedNumbers = new HashSet<String>();
            MatrixCursor matrix = new MatrixCursor(PROJECTION);
            while (cursor.moveToNext()) {
                // Key is phone number
                String phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));

                // Filter number already treated
                if (!treatedNumbers.contains(phoneNumber)) {
                    matrix.addRow(new Object[] {
                            cursor.getLong(cursor.getColumnIndex(Phone._ID)), phoneNumber,
                            cursor.getString(cursor.getColumnIndex(Phone.LABEL)),
                            cursor.getInt(cursor.getColumnIndex(Phone.TYPE)),
                            cursor.getLong(cursor.getColumnIndex(Phone.CONTACT_ID))
                    });
                    treatedNumbers.add(phoneNumber);
                }
            }
            return new ContactListAdapter(context, matrix);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "createContactListAdapter", e);
            }
            return null;
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
        ApiConnectionManager apiConnectionManager = ApiConnectionManager.getInstance(context);
        ContactUtils contactUtils = ContactUtils.getInstance(context);
        MatrixCursor matrix = new MatrixCursor(PROJECTION);
        ContactsService contactsApi = apiConnectionManager.getContactsApi();
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
                cursor = content.query(Phone.CONTENT_URI, PROJECTION, WHERE_CLAUSE, null, null);
                Set<ContactId> treatedContactIDs = new HashSet<ContactId>();

                while (cursor.moveToNext()) {
                    // Keep a trace of already treated row
                    String phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                    try {
                        ContactId contact = contactUtils.formatContact(phoneNumber);
                        // If this number is RCS and not already in the list,
                        // take it
                        if (rcsContactIds.contains(contact) && !treatedContactIDs.contains(contact)) {
                            matrix.addRow(new Object[] {
                                    cursor.getLong(cursor.getColumnIndex(Phone._ID)), phoneNumber,
                                    cursor.getString(cursor.getColumnIndex(Phone.LABEL)),
                                    cursor.getInt(cursor.getColumnIndex(Phone.TYPE)),
                                    cursor.getLong(cursor.getColumnIndex(Phone.CONTACT_ID))
                            });
                            treatedContactIDs.add(contact);
                        }
                    } catch (RcsContactFormatException e) {
                        // No a valid phone number: skip it
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
        TextView view = (TextView)inflater.inflate(R.layout.utils_spinner_item, parent, false);
        view.setTag(new ViewHolder(view, cursor));
        return view;
    }

    @Override
    public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        TextView view = (TextView)inflater.inflate(android.R.layout.simple_dropdown_item_1line,
                parent, false);
        view.setTag(new ViewHolder(view, cursor));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder)view.getTag();
        ((TextView)view).setText(formatText(context, cursor, holder));
        // Put the number in tag so it can be retrieved easily
        holder.number = cursor.getString(holder.columnNumber);
    }

    /**
     * Format the item to be displayed. The user name + label is displayed if
     * not null, else the phone number is used
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
        // Get contact name from contact id
        Cursor personCursor = context.getContentResolver().query(Contacts.CONTENT_URI,
                new String[] {
                    Contacts.DISPLAY_NAME
                }, Contacts._ID + " = " + c.getLong(holder.columnContactId), null, null);
        if (personCursor.moveToFirst()) {
            name = personCursor.getString(holder.columnID);
        }
        personCursor.close();
        if (name == null) {
            // Return "phone number"
            return c.getString(holder.columnNumber);
        } else {
            // Return "name (phone label)"
            return name + " (" + label + " )";
        }
    }

    /**
     * A ViewHolder class keeps references to children views to avoid
     * unnecessary calls to getColumnIndex() on each row.
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
            columnID = cursor.getColumnIndex(Phone._ID);
            columnNumber = cursor.getColumnIndex(Phone.NUMBER);
            columnLabel = cursor.getColumnIndex(Phone.LABEL);
            columnType = cursor.getColumnIndex(Phone.TYPE);
            columnContactId = cursor.getColumnIndex(Phone.CONTACT_ID);
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
        final ViewHolder holder = (ViewHolder)view.getTag();
        return holder.number;
    }
}
