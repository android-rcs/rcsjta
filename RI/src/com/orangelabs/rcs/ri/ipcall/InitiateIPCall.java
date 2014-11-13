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
package com.orangelabs.rcs.ri.ipcall;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate an IP call
 * 
 * @author Jean-Marc AUFFRET
 */
public class InitiateIPCall extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.ipcall_initiate);

		// Set title
		setTitle(R.string.menu_initiate_ipcall);
		
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
			Spinner spinner = (Spinner)findViewById(R.id.contact);
			MatrixCursor cursor = (MatrixCursor) spinner.getSelectedItem();
            ContactId contact = null;
            ContactUtils contactUtils = ContactUtils.getInstance(InitiateIPCall.this);
    		try {
    			contact = contactUtils.formatContact(cursor.getString(1));
    		} catch (RcsContactFormatException e1) {
    			Utils.showMessage(InitiateIPCall.this, getString(R.string.label_invalid_contact,cursor.getString(1)));
    	    	return;
    		}
    		
            // Get video option
	        CheckBox videoCheck = (CheckBox)findViewById(R.id.video);
            
			// Display session view
			Intent intent = new Intent(InitiateIPCall.this, IPCallView.class);
        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	intent.putExtra(IPCallView.EXTRA_MODE, IPCallView.MODE_OUTGOING);
        	intent.putExtra(IPCallView.EXTRA_CONTACT, (Parcelable)contact);
        	intent.putExtra(IPCallView.EXTRA_VIDEO_OPTION, videoCheck.isChecked());
			startActivity(intent);
			
        	// Exit activity
        	finish();     
		}
	};
}