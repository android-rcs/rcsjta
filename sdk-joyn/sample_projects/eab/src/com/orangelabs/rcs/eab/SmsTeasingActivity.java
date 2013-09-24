/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Data;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Teasing dialog for non RCS contact
 */
public class SmsTeasingActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                       
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.rcs_eab_sms_teasing);
        
        // Set title
        setTitle(R.string.rcs_eab_smsTeasingActivityTitle);
        
        // Get the corresponding contact from the intent
        Intent intent = getIntent();
        Uri contactUri = intent.getData();
    	if (contactUri != null) {
	        Cursor cursor = managedQuery(contactUri, null, null, null, null);
	        if (cursor.moveToNext()) {
    			selectedContact = cursor.getString(cursor.getColumnIndex(Data.DATA1));
		        TextView msgView = (TextView)findViewById(R.id.msg);
		        msgView.setText(getString(R.string.rcs_eab_not_rcs_capable, selectedContact));
	        }
	        cursor.close();
        }
    	
    	Button sendSmsButton = (Button)findViewById(R.id.send_sms_button);
    	sendSmsButton.setOnClickListener(sendSMSListener);
    }
    
    /**
     * The selected contact
     */
    String selectedContact;
    
    /**
	 * We clicked on the send SMS button
	 */
	private OnClickListener sendSMSListener = new OnClickListener(){
		public void onClick(View view) {
			Intent sendIntent = new Intent(Intent.ACTION_VIEW);
			sendIntent.putExtra("sms_body", getString(R.string.rcs_eab_sms_teasing_text)); 
			sendIntent.putExtra("address", selectedContact);
			sendIntent.setType("vnd.android-dir/mms-sms");
			startActivity(sendIntent);  
		}
	};
}

