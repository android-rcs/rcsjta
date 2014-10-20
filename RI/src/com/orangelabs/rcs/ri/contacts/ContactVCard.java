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
package com.orangelabs.rcs.ri.contacts;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.gsma.services.rcs.contacts.ContactsService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Display contact VCard
 *  
 * @author Jean-Marc AUFFRET
 */
public class ContactVCard extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_vcard);
        
        // Set title
        setTitle(R.string.menu_contact_vcard);
        
        // Set the contact selector
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        spinner.setAdapter(Utils.createContactListAdapter(this));
        spinner.setOnItemSelectedListener(listenerContact);

        // Set button callback
        Button showBtn = (Button)findViewById(R.id.show_btn);
        showBtn.setOnClickListener(btnShowListener);

        // Get selected contact
    	String contact = getSelectedContact(); 

    	// Update UI
		displayVisitCard(contact);
    }
    
    /**
     * Spinner contact listener
     */
    private OnItemSelectedListener listenerContact = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
	        // Get selected contact
	    	String contact = getContactAtPosition(position); 

	    	// Update UI
			displayVisitCard(contact);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	};
	
    /**
     * Returns the contact at given position
     * 
     * @param position Position in the adapter
     * @return Contact
     */
    private String getContactAtPosition(int position) {
	    Spinner spinner = (Spinner)findViewById(R.id.contact);
	    MatrixCursor cursor = (MatrixCursor)spinner.getItemAtPosition(position);
	    return cursor.getString(1);
    }
    
    /**
     * Returns the selected contact
     * 
     * @param position Position in the adapter
     * @return Contact
     */
    private String getSelectedContact() {
	    Spinner spinner = (Spinner)findViewById(R.id.contact);
	    MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
	    return cursor.getString(1);
    }

    /**
     * Display the visit card
     * 
     * @param contact Contact
     */
    private void displayVisitCard(String contact) {
    	try {
    		Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contact));
			String vcard = ContactsService.getVCard(this, contactUri);
			TextView vcardView = (TextView)findViewById(R.id.vcard);
		    vcardView.setText(vcard);
		} catch(Exception e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(ContactVCard.this, getString(R.string.label_api_failed));
		}
    }
    
    /**
     * Show button listener
     */
    private OnClickListener btnShowListener = new OnClickListener() {
        public void onClick(View v) {
        	// Get filename
			TextView vcardView = (TextView)findViewById(R.id.vcard);
			String filename = vcardView.getText().toString();
					
        	// Show the transferred vCard
        	File file = new File(filename);
        	Uri uri = Uri.fromFile(file);
        	Intent intent = new Intent(Intent.ACTION_VIEW);
        	intent.setDataAndType(uri, "text/plain");   		
        	startActivity(intent);
    	}
	};
 }
