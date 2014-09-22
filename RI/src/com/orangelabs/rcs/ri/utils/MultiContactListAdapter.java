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
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;
import android.widget.Filterable;

/**
 * Contact check adapter 
 */
public class MultiContactListAdapter extends CursorAdapter implements Filterable {
	
	/**
	 * Constructor
	 * 
	 * @param context Context
	 * @param c Cursor
	 */
	public MultiContactListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        CheckedTextView view = (CheckedTextView)inflater.inflate(
                android.R.layout.simple_list_item_multiple_choice, parent, false);
        view.setText(formatText(context, cursor));
        view.setSelected(true);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
         ((CheckedTextView)view).setText(formatText(context, cursor));
         // Put the number in tag so it can be retrieved easily
         view.setTag(cursor.getString(1));
    }
    
    /**
     * Format the item to be displayed
     * 
     * @param context Context
     * @param c Cursor
     * @return String
     */
    private String formatText(Context context, Cursor c) {
    	// The user name + label is displayed if not null, else the phone number is used
    	// Get phone label
    	String label = c.getString(2);
    	if (label==null){
			// Label is not custom, get the string corresponding to the phone type
			int type = c.getInt(3);
			label = context.getString(Phone.getTypeLabelResource(type));
		}
    	
    	String name = null;
    	// Get contact name from contact id
    	Cursor personCursor = context.getContentResolver().query(Contacts.CONTENT_URI, 
    			new String[]{Contacts.DISPLAY_NAME}, 
    			Contacts._ID + " = " + c.getLong(4), 
    			null, 
    			null);
    	if (personCursor.moveToFirst()){
    		name = personCursor.getString(0);
    	}
    	personCursor.close();
    	if (name==null){
    		// Return "phone number"
    		return c.getString(1);
    	}else{
    		// Return "name (phone label)"
    		return name + " (" + label + " )";
    	}
    }
    
}
