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
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Multimedia session settings
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSessionSettings extends Activity {
	/**
	 * CRLF constant
	 */
	public final static String CRLF = "\r\n";
	
	/**
	 * Preferences URI
	 */
	public static final String APP_PREFERENCES = "SipDemo";
	
	/**
	 * SDP settings
	 */
	public static final String SETTINGS_SDP = "Sdp";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.session_settings);

		// Set title
		setTitle(R.string.menu_mm_session_settings);
		
		// Set text listeners
		EditText sdpEdit = (EditText)findViewById(R.id.local_sdp);
		sdpEdit.setOnTouchListener(onEditTextTouchListener);
		
		// Set buttons callback
		Button saveBtn = (Button)findViewById(R.id.save_btn);
		saveBtn.setOnClickListener(btnSaveListener);
		Button restoreBtn = (Button)findViewById(R.id.restore_btn);
		restoreBtn.setOnClickListener(btnRestoreListener);
		
		// Show settings
		showSettings();
	}

	/**
	 * Returns local SDP
	 * 
	 * @param ctx Context
	 * @return SDP
	 */
	public static String getLocalSdp(Context ctx) {
		String ntpTime = Utils.constructNTPtime(System.currentTimeMillis());
		String myIpAddress = Utils.getLocalIpAddress();
		String sdp =
    		"v=0" + CRLF +
            "o=- " + ntpTime + " " + ntpTime + " IN IP4 " + myIpAddress + CRLF +
            "s=SIPDEMO" + CRLF +
			"c=IN IP4 " + myIpAddress + CRLF +
            "t=0 0" + CRLF +
            "m=audio 5000 RTP/AVP 96" + CRLF + 
            "a=rtpmap:96 AMR" + CRLF;
		SharedPreferences preferences = ctx.getSharedPreferences(MultimediaSessionSettings.APP_PREFERENCES, MODE_PRIVATE);	
		return preferences.getString(MultimediaSessionSettings.SETTINGS_SDP, sdp);
	}
	
	/**
	 * Set local SDP
	 * 
	 * @param ctx Context
	 * @param sdp SDP
	 */
	public static void setLocalSdp(Context ctx, String sdp) {
		SharedPreferences preferences = ctx.getSharedPreferences(MultimediaSessionSettings.APP_PREFERENCES, MODE_PRIVATE);	
		Editor editor = preferences.edit();
		if (sdp == null) {
			editor.remove(SETTINGS_SDP);
		} else {
			editor.putString(SETTINGS_SDP, sdp);
		}
		editor.commit();
	}
	
	/**
	 * Show settings
	 */
	public void showSettings() {
		EditText sdpEdit = (EditText)findViewById(R.id.local_sdp);
		sdpEdit.setText(getLocalSdp(this));
	}
	
	/**
	 * Save button callback
	 */
	private OnClickListener btnSaveListener = new OnClickListener() {
		public void onClick(View v) {
			// Save SDP values
			EditText sdpEdit = (EditText)findViewById(R.id.local_sdp);
			setLocalSdp(MultimediaSessionSettings.this, sdpEdit.getText().toString());
			Toast.makeText(MultimediaSessionSettings.this, R.string.label_settings_saved, Toast.LENGTH_SHORT).show();
		}
	};

	/**
	 * Restore button callback
	 */
	private OnClickListener btnRestoreListener = new OnClickListener() {
		public void onClick(View v) {
			// Restore default SDP values
			setLocalSdp(MultimediaSessionSettings.this, null);
			Toast.makeText(MultimediaSessionSettings.this, R.string.label_settings_restored, Toast.LENGTH_SHORT).show();
			
			// Show settings
			showSettings();			
		}
	};

	/**
	 * Display a dialog when the text edit is touched
	 */
	private OnTouchListener onEditTextTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				// Touched editText
				final EditText externalEditText = (EditText)v;
				
				// Display dialog
				final EditText internalEditText = new EditText(MultimediaSessionSettings.this);
				AlertDialog.Builder builder = new Builder(MultimediaSessionSettings.this);
				internalEditText.setText(externalEditText.getText().toString());
				builder.setView(internalEditText);
				builder.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						externalEditText.setText(internalEditText.getText().toString());
						InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_NOT_ALWAYS);
					}
				});
				builder.setNegativeButton(R.string.label_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,	int which) {
						InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_NOT_ALWAYS);
					}
				});
				builder.show();
			}
			return true;
		}
	};
}