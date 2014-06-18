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

import static com.orangelabs.rcs.provisioning.local.Provisioning.saveCheckBoxParameter;
import static com.orangelabs.rcs.provisioning.local.Provisioning.saveEditTextParameter;
import static com.orangelabs.rcs.provisioning.local.Provisioning.setCheckBoxParameter;
import static com.orangelabs.rcs.provisioning.local.Provisioning.setEditTextParameter;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
	private static final String[] IM_SESSION_START_MODES = { "0", "1", "2" };
	
	private boolean isInFront;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		// Set layout
		setContentView(R.layout.rcs_provisioning_service);

		// Set buttons callback
		Button btn = (Button) findViewById(R.id.save_btn);
		btn.setOnClickListener(saveBtnListener);
		updateView(bundle);
		isInFront = true;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (isInFront == false) {
			isInFront = true;
			// Update UI (from DB)
			updateView(null);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		isInFront = false;
	}

	/**
	 * Update view
	 * @param bundle
	 */
	private void updateView(Bundle bundle) {
		// Display UI parameters
		setEditTextParameter(this, R.id.MaxPhotoIconSize, RcsSettingsData.MAX_PHOTO_ICON_SIZE, bundle);
		setEditTextParameter(this, R.id.MaxFreetextLength, RcsSettingsData.MAX_FREETXT_LENGTH, bundle);
		setEditTextParameter(this, R.id.MaxChatParticipants, RcsSettingsData.MAX_CHAT_PARTICIPANTS, bundle);
		setEditTextParameter(this, R.id.MaxChatMessageLength, RcsSettingsData.MAX_CHAT_MSG_LENGTH, bundle);
		setEditTextParameter(this, R.id.MaxGroupChatMessageLength, RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH, bundle);
		setEditTextParameter(this, R.id.ChatIdleDuration, RcsSettingsData.CHAT_IDLE_DURATION, bundle);
		setEditTextParameter(this, R.id.MaxFileTransferSize, RcsSettingsData.MAX_FILE_TRANSFER_SIZE, bundle);
		setEditTextParameter(this, R.id.WarnFileTransferSize, RcsSettingsData.WARN_FILE_TRANSFER_SIZE, bundle);
		setEditTextParameter(this, R.id.MaxImageShareSize, RcsSettingsData.MAX_IMAGE_SHARE_SIZE, bundle);
		setEditTextParameter(this, R.id.MaxVideoShareDuration, RcsSettingsData.MAX_VIDEO_SHARE_DURATION, bundle);
		setEditTextParameter(this, R.id.MaxChatSessions, RcsSettingsData.MAX_CHAT_SESSIONS, bundle);
		setEditTextParameter(this, R.id.MaxFileTransferSessions, RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS, bundle);
		setEditTextParameter(this, R.id.MaxIpCallSessions, RcsSettingsData.MAX_IP_CALL_SESSIONS, bundle);
		setEditTextParameter(this, R.id.MaxChatLogEntries, RcsSettingsData.MAX_CHAT_LOG_ENTRIES, bundle);
		setEditTextParameter(this, R.id.MaxRichcallLogEntries, RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES, bundle);
		setEditTextParameter(this, R.id.MaxIpcallLogEntries, RcsSettingsData.MAX_IPCALL_LOG_ENTRIES, bundle);
		setEditTextParameter(this, R.id.DirectoryPathPhotos, RcsSettingsData.DIRECTORY_PATH_PHOTOS, bundle);
		setEditTextParameter(this, R.id.DirectoryPathVideos, RcsSettingsData.DIRECTORY_PATH_VIDEOS, bundle);
		setEditTextParameter(this, R.id.DirectoryPathFiles, RcsSettingsData.DIRECTORY_PATH_FILES, bundle);
		setEditTextParameter(this, R.id.MaxGeolocLabelLength, RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH, bundle);
		setEditTextParameter(this, R.id.GeolocExpirationTime, RcsSettingsData.GEOLOC_EXPIRATION_TIME, bundle);

		Spinner spinner = (Spinner) findViewById(R.id.ImSessionStart);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, IM_SESSION_START_MODES);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		Provisioning.setSpinnerParameter(spinner, RcsSettingsData.IM_SESSION_START, bundle, IM_SESSION_START_MODES);

		setCheckBoxParameter(this, R.id.SmsFallbackService, RcsSettingsData.SMS_FALLBACK_SERVICE, bundle);
		setCheckBoxParameter(this, R.id.StoreForwardServiceWarning, RcsSettingsData.WARN_SF_SERVICE, bundle);
		setCheckBoxParameter(this, R.id.AutoAcceptFileTransfer, RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER, bundle);
		setCheckBoxParameter(this, R.id.AutoAcceptChat, RcsSettingsData.AUTO_ACCEPT_CHAT, bundle);
		setCheckBoxParameter(this, R.id.AutoAcceptGroupChat, RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT, bundle);
	}

	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		saveInstanceState(bundle);
	}

	/**
	 * Save parameters either in bundle or in RCS settings
	 */
	private void saveInstanceState(Bundle bundle) {
		saveEditTextParameter(this, R.id.MaxPhotoIconSize, RcsSettingsData.MAX_PHOTO_ICON_SIZE, bundle);
		saveEditTextParameter(this, R.id.MaxFreetextLength, RcsSettingsData.MAX_FREETXT_LENGTH, bundle);
		saveEditTextParameter(this, R.id.MaxChatParticipants, RcsSettingsData.MAX_CHAT_PARTICIPANTS, bundle);
		saveEditTextParameter(this, R.id.MaxChatMessageLength, RcsSettingsData.MAX_CHAT_MSG_LENGTH, bundle);
		saveEditTextParameter(this, R.id.MaxGroupChatMessageLength, RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH, bundle);
		saveEditTextParameter(this, R.id.ChatIdleDuration, RcsSettingsData.CHAT_IDLE_DURATION, bundle);
		saveEditTextParameter(this, R.id.MaxFileTransferSize, RcsSettingsData.MAX_FILE_TRANSFER_SIZE, bundle);
		saveEditTextParameter(this, R.id.WarnFileTransferSize, RcsSettingsData.WARN_FILE_TRANSFER_SIZE, bundle);
		saveEditTextParameter(this, R.id.MaxImageShareSize, RcsSettingsData.MAX_IMAGE_SHARE_SIZE, bundle);
		saveEditTextParameter(this, R.id.MaxVideoShareDuration, RcsSettingsData.MAX_VIDEO_SHARE_DURATION, bundle);
		saveEditTextParameter(this, R.id.MaxChatSessions, RcsSettingsData.MAX_CHAT_SESSIONS, bundle);
		saveEditTextParameter(this, R.id.MaxFileTransferSessions, RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS, bundle);
		saveEditTextParameter(this, R.id.MaxIpCallSessions, RcsSettingsData.MAX_IP_CALL_SESSIONS, bundle);
		saveEditTextParameter(this, R.id.MaxChatLogEntries, RcsSettingsData.MAX_CHAT_LOG_ENTRIES, bundle);
		saveEditTextParameter(this, R.id.MaxRichcallLogEntries, RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES, bundle);
		saveEditTextParameter(this, R.id.MaxIpcallLogEntries, RcsSettingsData.MAX_IPCALL_LOG_ENTRIES, bundle);
		saveEditTextParameter(this, R.id.MaxGeolocLabelLength, RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH, bundle);
		saveEditTextParameter(this, R.id.GeolocExpirationTime, RcsSettingsData.GEOLOC_EXPIRATION_TIME, bundle);
		saveEditTextParameter(this, R.id.DirectoryPathPhotos, RcsSettingsData.DIRECTORY_PATH_PHOTOS, bundle);
		saveEditTextParameter(this, R.id.DirectoryPathVideos, RcsSettingsData.DIRECTORY_PATH_VIDEOS, bundle);
		saveEditTextParameter(this, R.id.DirectoryPathFiles, RcsSettingsData.DIRECTORY_PATH_FILES, bundle);

		Spinner spinner = (Spinner) findViewById(R.id.ImSessionStart);
		if (bundle != null) {
			bundle.putInt(RcsSettingsData.IM_SESSION_START, spinner.getSelectedItemPosition());
		} else {
			RcsSettings.getInstance().writeInteger(RcsSettingsData.IM_SESSION_START, spinner.getSelectedItemPosition());
		}

		saveCheckBoxParameter(this, R.id.SmsFallbackService, RcsSettingsData.SMS_FALLBACK_SERVICE, bundle);
		saveCheckBoxParameter(this, R.id.StoreForwardServiceWarning, RcsSettingsData.WARN_SF_SERVICE, bundle);
		saveCheckBoxParameter(this, R.id.AutoAcceptFileTransfer, RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER, bundle);
		saveCheckBoxParameter(this, R.id.AutoAcceptChat, RcsSettingsData.AUTO_ACCEPT_CHAT, bundle);
		saveCheckBoxParameter(this, R.id.AutoAcceptGroupChat, RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT, bundle);
	}

	/**
	 * Save button listener
	 */
	private OnClickListener saveBtnListener = new OnClickListener() {
		public void onClick(View v) {
			// Save parameters
			saveInstanceState(null);
		}
	};
}
