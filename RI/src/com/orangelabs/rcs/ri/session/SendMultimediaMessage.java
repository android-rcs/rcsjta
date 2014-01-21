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
package com.orangelabs.rcs.ri.session;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.session.MultimediaSessionService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Send a multimedia message
 * 
 * @author Jean-Marc AUFFRET
 */
public class SendMultimediaMessage extends Activity implements JoynServiceListener {
	/**
	 * MM session API
	 */
	private MultimediaSessionService sessionApi;

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.session_send_msg);

		// Set title
		setTitle(R.string.menu_send_mm_message);
		
		// Set contact selector
		Spinner spinner = (Spinner)findViewById(R.id.contact);
		spinner.setAdapter(Utils.createContactListAdapter(this));

		// Set buttons callback
		Button sendBtn = (Button)findViewById(R.id.send_btn);
		sendBtn.setOnClickListener(btnSendListener);
		if (spinner.getAdapter().getCount() == 0) {
			sendBtn.setEnabled(false);
		}

        // Instanciate API
        sessionApi = new MultimediaSessionService(getApplicationContext(), this);
        
        // Connect API
        sessionApi.connect();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();

        // Disconnect API
        sessionApi.disconnect();
	}

    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        // Disable button if no contact available
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        Button sendBtn = (Button)findViewById(R.id.send_btn);
        if (spinner.getAdapter().getCount() != 0) {
        	sendBtn.setEnabled(true);
        }
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(SendMultimediaMessage.this, getString(R.string.label_api_disabled));
    }    

	/**
	 * Send button callback
	 */
	private OnClickListener btnSendListener = new OnClickListener() {
		public void onClick(View v) {
			// Get remote contact
			Spinner spinner = (Spinner)findViewById(R.id.contact);
			MatrixCursor cursor = (MatrixCursor) spinner.getSelectedItem();
			final String contact = cursor.getString(1);

			// Get content
			TextView contentView = (TextView)findViewById(R.id.content);
			final String content = contentView.getText().toString();
			
			// Get content type
			TextView typeView = (TextView)findViewById(R.id.type);
			final String contentType = typeView.getText().toString();

			// Initiate session in background
			Thread thread = new Thread() {
				public void run() {
					try {
						// Initiate session
						boolean result = sessionApi.sendMessage(TestMultimediaSessionApi.SERVICE_ID, contact, content, contentType);
						if (result) {
							Utils.displayToast(SendMultimediaMessage.this, getString(R.string.label_mm_msg_success));
						} else {
							Utils.displayToast(SendMultimediaMessage.this, getString(R.string.label_mm_msg_failed));
						}
					} catch (Exception e) {
						Utils.displayToast(SendMultimediaMessage.this, getString(R.string.label_mm_msg_failed));
					}
				}
			};
			thread.start();
			Utils.displayToast(SendMultimediaMessage.this, getString(R.string.label_mm_msg_sent));
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}