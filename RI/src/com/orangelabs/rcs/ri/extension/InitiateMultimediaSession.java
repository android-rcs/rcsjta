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
package com.orangelabs.rcs.ri.extension;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Abstract class to initiate a multimedia session
 *  
 * @author Jean-Marc AUFFRET
 */
public abstract class InitiateMultimediaSession extends Activity {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(InitiateMultimediaSession.class.getSimpleName());
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.extension_initiate_session);

		// Set contact selector
		Spinner spinner = (Spinner)findViewById(R.id.contact);
		spinner.setAdapter(Utils.createContactListAdapter(this));

		// Set buttons callback
		Button initiateBtn = (Button)findViewById(R.id.initiate_btn);
		initiateBtn.setOnClickListener(btnInitiateListener);

        // Disable button if no contact available
        if (spinner.getAdapter().getCount() == 0) {
        	initiateBtn.setEnabled(false);
        }
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Initiate button callback
	 */
	private OnClickListener btnInitiateListener = new OnClickListener() {
		public void onClick(View v) {
			// Get remote contact
			Spinner spinner = (Spinner) findViewById(R.id.contact);
			MatrixCursor cursor = (MatrixCursor) spinner.getSelectedItem();
			String remoteContact = cursor.getString(1);
			try {
				ContactUtils contactUtils = ContactUtils.getInstance(InitiateMultimediaSession.this);
				ContactId contact = contactUtils.formatContact(remoteContact);
				// Initiate session
				initiateSession(contact);
			} catch (RcsContactFormatException e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Cannot parse contact " + remoteContact);
				}
			} finally {
				// Exit activity
				finish();
			}
		}
	};
	
	/**
	 * Initiate session
	 * 
	 * @param contact Remote contact
	 */
	public abstract void initiateSession(ContactId contact);
}