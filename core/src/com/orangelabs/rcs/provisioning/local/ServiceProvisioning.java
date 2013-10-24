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

package com.orangelabs.rcs.provisioning.local;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;

/**
 * Service parameters provisioning
 * 
 * @author jexa7410
 */
public class ServiceProvisioning extends Activity {
	/**
	 * IM session start modes
	 */
    private static final String[] IM_SESSION_START_MODES = {
    	"1", "2"
    };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.rcs_provisioning_service);
        
		// Set buttons callback
        Button btn = (Button)findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);        
	}

	@Override
	protected void onResume() {
		super.onResume();
		
        // Display UI parameters

        EditText txt = (EditText)this.findViewById(R.id.MaxPhotoIconSize);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_PHOTO_ICON_SIZE));

        txt = (EditText)this.findViewById(R.id.MaxFreetextLength);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_FREETXT_LENGTH));

        txt = (EditText)this.findViewById(R.id.MaxChatParticipants);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_CHAT_PARTICIPANTS));

        txt = (EditText)this.findViewById(R.id.MaxChatMessageLength);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_CHAT_MSG_LENGTH));

        txt = (EditText)this.findViewById(R.id.MaxGroupChatMessageLength);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH));

        txt = (EditText)this.findViewById(R.id.ChatIdleDuration);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.CHAT_IDLE_DURATION));

        txt = (EditText)this.findViewById(R.id.MaxFileTransferSize);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_FILE_TRANSFER_SIZE));

        txt = (EditText)this.findViewById(R.id.WarnFileTransferSize);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.WARN_FILE_TRANSFER_SIZE));

        txt = (EditText)this.findViewById(R.id.MaxImageShareSize);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_IMAGE_SHARE_SIZE));

        txt = (EditText)this.findViewById(R.id.MaxVideoShareDuration);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_VIDEO_SHARE_DURATION));

        txt = (EditText)this.findViewById(R.id.MaxChatSessions);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_CHAT_SESSIONS));

        txt = (EditText)this.findViewById(R.id.MaxFileTransferSessions);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS));
        
        txt = (EditText)this.findViewById(R.id.MaxIpCallSessions);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_IP_CALL_SESSIONS));

        txt = (EditText)this.findViewById(R.id.MaxChatLogEntries);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_CHAT_LOG_ENTRIES));

        txt = (EditText)this.findViewById(R.id.MaxRichcallLogEntries);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES));
        
        txt = (EditText)this.findViewById(R.id.MaxIpcallLogEntries);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_IPCALL_LOG_ENTRIES));

        txt = (EditText)this.findViewById(R.id.DirectoryPathPhotos);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.DIRECTORY_PATH_PHOTOS));

        txt = (EditText)this.findViewById(R.id.DirectoryPathVideos);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.DIRECTORY_PATH_VIDEOS));

        txt = (EditText)this.findViewById(R.id.DirectoryPathFiles);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.DIRECTORY_PATH_FILES));

        txt = (EditText)this.findViewById(R.id.MaxGeolocLabelLength);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH));
        
        txt = (EditText)this.findViewById(R.id.GeolocExpirationTime);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.GEOLOC_EXPIRATION_TIME));
        
        Spinner spinner = (Spinner)findViewById(R.id.ImSessionStart);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, IM_SESSION_START_MODES);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		if (RcsSettings.getInstance().getImSessionStartMode() == 1) {
			spinner.setSelection(0);
		} else {
			spinner.setSelection(1);
		}
        
        CheckBox check = (CheckBox)this.findViewById(R.id.SmsFallbackService);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.SMS_FALLBACK_SERVICE)));

        check = (CheckBox)this.findViewById(R.id.StoreForwardServiceWarning);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.WARN_SF_SERVICE)));

        check = (CheckBox)this.findViewById(R.id.AutoAcceptFileTransfer);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER)));

        check = (CheckBox)this.findViewById(R.id.AutoAcceptChat);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.AUTO_ACCEPT_CHAT)));

        check = (CheckBox)this.findViewById(R.id.AutoAcceptGroupChat);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT)));
	}
	
    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
	        // Save parameters
        	save();
        }
    };
    
    /**
     * Save parameters
     */
    private void save() {
        EditText txt = (EditText)this.findViewById(R.id.MaxPhotoIconSize);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_PHOTO_ICON_SIZE, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxFreetextLength);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_FREETXT_LENGTH, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxChatParticipants);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_CHAT_PARTICIPANTS, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxChatMessageLength);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_CHAT_MSG_LENGTH, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxGroupChatMessageLength);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH, txt.getText().toString());

		txt = (EditText)this.findViewById(R.id.ChatIdleDuration);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.CHAT_IDLE_DURATION, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxFileTransferSize);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_FILE_TRANSFER_SIZE, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.WarnFileTransferSize);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.WARN_FILE_TRANSFER_SIZE, txt.getText().toString());

		txt = (EditText)this.findViewById(R.id.MaxImageShareSize);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_IMAGE_SHARE_SIZE, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxVideoShareDuration);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_VIDEO_SHARE_DURATION, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxChatSessions);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_CHAT_SESSIONS, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxFileTransferSessions);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxIpCallSessions);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_IP_CALL_SESSIONS, txt.getText().toString());

		txt = (EditText)this.findViewById(R.id.MaxChatLogEntries);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_CHAT_LOG_ENTRIES, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.MaxRichcallLogEntries);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES, txt.getText().toString());
		
        txt = (EditText)this.findViewById(R.id.MaxIpcallLogEntries);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_IPCALL_LOG_ENTRIES, txt.getText().toString());
		
        txt = (EditText)this.findViewById(R.id.MaxGeolocLabelLength);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.GeolocExpirationTime);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.GEOLOC_EXPIRATION_TIME, txt.getText().toString());
		
        txt = (EditText)this.findViewById(R.id.DirectoryPathPhotos);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.DIRECTORY_PATH_PHOTOS, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.DirectoryPathVideos);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.DIRECTORY_PATH_VIDEOS, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.DirectoryPathFiles);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.DIRECTORY_PATH_FILES, txt.getText().toString());

		Spinner spinner = (Spinner)findViewById(R.id.ImSessionStart);
		if (spinner.getSelectedItemId() == 0) {
			RcsSettings.getInstance().writeParameter(RcsSettingsData.IM_SESSION_START, "1");
		} else {
			RcsSettings.getInstance().writeParameter(RcsSettingsData.IM_SESSION_START, "2");
		}
		
		CheckBox check = (CheckBox)this.findViewById(R.id.SmsFallbackService);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SMS_FALLBACK_SERVICE, Boolean.toString(check.isChecked()));

        check = (CheckBox)this.findViewById(R.id.StoreForwardServiceWarning);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.WARN_SF_SERVICE, Boolean.toString(check.isChecked()));
		
        check = (CheckBox)this.findViewById(R.id.AutoAcceptFileTransfer);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER, Boolean.toString(check.isChecked()));
		
        check = (CheckBox)this.findViewById(R.id.AutoAcceptChat);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.AUTO_ACCEPT_CHAT, Boolean.toString(check.isChecked()));

        check = (CheckBox)this.findViewById(R.id.AutoAcceptGroupChat);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT, Boolean.toString(check.isChecked()));
	}
}
