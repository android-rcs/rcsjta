/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provisioning.local;

import static com.gsma.rcs.provisioning.local.Provisioning.saveCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setEditTextParam;

import com.gsma.rcs.R;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

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
            "0", "1", "2"
    };

    private boolean mInFront;

    private RcsSettings mRcsSettings;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // Set layout
        setContentView(R.layout.rcs_provisioning_service);

        // Set buttons callback
        Button btn = (Button) findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        mRcsSettings = RcsSettings.createInstance(new LocalContentResolver(this));
        updateView(bundle);
        mInFront = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mInFront == false) {
            mInFront = true;
            // Update UI (from DB)
            updateView(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mInFront = false;
    }

    /**
     * Update view
     * 
     * @param bundle
     */
    private void updateView(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        // Display UI parameters
        setEditTextParam(R.id.MaxPhotoIconSize, RcsSettingsData.MAX_PHOTO_ICON_SIZE, helper);
        setEditTextParam(R.id.MaxFreetextLength, RcsSettingsData.MAX_FREETXT_LENGTH, helper);
        setEditTextParam(R.id.MaxChatParticipants, RcsSettingsData.MAX_CHAT_PARTICIPANTS, helper);
        setEditTextParam(R.id.MaxChatMessageLength, RcsSettingsData.MAX_CHAT_MSG_LENGTH, helper);
        setEditTextParam(R.id.MaxGroupChatMessageLength, RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH,
                helper);
        setEditTextParam(R.id.ChatIdleDuration, RcsSettingsData.CHAT_IDLE_DURATION, helper);
        setEditTextParam(R.id.MaxFileTransferSize, RcsSettingsData.MAX_FILE_TRANSFER_SIZE, helper);
        setEditTextParam(R.id.WarnFileTransferSize, RcsSettingsData.WARN_FILE_TRANSFER_SIZE, helper);
        setEditTextParam(R.id.MaxImageShareSize, RcsSettingsData.MAX_IMAGE_SHARE_SIZE, helper);
        setEditTextParam(R.id.MaxVideoShareDuration, RcsSettingsData.MAX_VIDEO_SHARE_DURATION,
                helper);
        setEditTextParam(R.id.MaxChatSessions, RcsSettingsData.MAX_CHAT_SESSIONS, helper);
        setEditTextParam(R.id.MaxFileTransferSessions, RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS,
                helper);
        setEditTextParam(R.id.MaxConcurrentOutgoingFileTransferSessions,
                RcsSettingsData.MAX_CONCURRENT_OUTGOING_FILE_TRANSFERS, helper);
        setEditTextParam(R.id.MaxIpCallSessions, RcsSettingsData.MAX_IP_CALL_SESSIONS, helper);
        setEditTextParam(R.id.MaxChatLogEntries, RcsSettingsData.MAX_CHAT_LOG_ENTRIES, helper);
        setEditTextParam(R.id.MaxRichcallLogEntries, RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES,
                helper);
        setEditTextParam(R.id.MaxIpcallLogEntries, RcsSettingsData.MAX_IPCALL_LOG_ENTRIES, helper);
        setEditTextParam(R.id.DirectoryPathPhotos, RcsSettingsData.DIRECTORY_PATH_PHOTOS, helper);
        setEditTextParam(R.id.DirectoryPathVideos, RcsSettingsData.DIRECTORY_PATH_VIDEOS, helper);
        setEditTextParam(R.id.DirectoryPathFiles, RcsSettingsData.DIRECTORY_PATH_FILES, helper);
        setEditTextParam(R.id.MaxGeolocLabelLength, RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH, helper);
        setEditTextParam(R.id.GeolocExpirationTime, RcsSettingsData.GEOLOC_EXPIRATION_TIME, helper);

        Spinner spinner = (Spinner) findViewById(R.id.ImSessionStart);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, IM_SESSION_START_MODES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        Provisioning.setSpinnerParameter(spinner, RcsSettingsData.IM_SESSION_START,
                IM_SESSION_START_MODES, helper);

        setCheckBoxParam(R.id.SmsFallbackService, RcsSettingsData.SMS_FALLBACK_SERVICE, helper);
        setCheckBoxParam(R.id.StoreForwardServiceWarning, RcsSettingsData.WARN_SF_SERVICE, helper);
        setCheckBoxParam(R.id.AutoAcceptFileTransfer, RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER,
                helper);
        setCheckBoxParam(R.id.AutoAcceptChat, RcsSettingsData.AUTO_ACCEPT_CHAT, helper);
        setCheckBoxParam(R.id.AutoAcceptGroupChat, RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT, helper);
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
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        saveEditTextParam(R.id.MaxPhotoIconSize, RcsSettingsData.MAX_PHOTO_ICON_SIZE, helper);
        saveEditTextParam(R.id.MaxFreetextLength, RcsSettingsData.MAX_FREETXT_LENGTH, helper);
        saveEditTextParam(R.id.MaxChatParticipants, RcsSettingsData.MAX_CHAT_PARTICIPANTS, helper);
        saveEditTextParam(R.id.MaxChatMessageLength, RcsSettingsData.MAX_CHAT_MSG_LENGTH, helper);
        saveEditTextParam(R.id.MaxGroupChatMessageLength, RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH,
                helper);
        saveEditTextParam(R.id.ChatIdleDuration, RcsSettingsData.CHAT_IDLE_DURATION, helper);
        saveEditTextParam(R.id.MaxFileTransferSize, RcsSettingsData.MAX_FILE_TRANSFER_SIZE, helper);
        saveEditTextParam(R.id.WarnFileTransferSize, RcsSettingsData.WARN_FILE_TRANSFER_SIZE,
                helper);
        saveEditTextParam(R.id.MaxImageShareSize, RcsSettingsData.MAX_IMAGE_SHARE_SIZE, helper);
        saveEditTextParam(R.id.MaxVideoShareDuration, RcsSettingsData.MAX_VIDEO_SHARE_DURATION,
                helper);
        saveEditTextParam(R.id.MaxChatSessions, RcsSettingsData.MAX_CHAT_SESSIONS, helper);
        saveEditTextParam(R.id.MaxFileTransferSessions, RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS,
                helper);
        saveEditTextParam(R.id.MaxConcurrentOutgoingFileTransferSessions,
                RcsSettingsData.MAX_CONCURRENT_OUTGOING_FILE_TRANSFERS, helper);
        saveEditTextParam(R.id.MaxIpCallSessions, RcsSettingsData.MAX_IP_CALL_SESSIONS, helper);
        saveEditTextParam(R.id.MaxChatLogEntries, RcsSettingsData.MAX_CHAT_LOG_ENTRIES, helper);
        saveEditTextParam(R.id.MaxRichcallLogEntries, RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES,
                helper);
        saveEditTextParam(R.id.MaxIpcallLogEntries, RcsSettingsData.MAX_IPCALL_LOG_ENTRIES, helper);
        saveEditTextParam(R.id.MaxGeolocLabelLength, RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH,
                helper);
        saveEditTextParam(R.id.GeolocExpirationTime, RcsSettingsData.GEOLOC_EXPIRATION_TIME, helper);
        saveEditTextParam(R.id.DirectoryPathPhotos, RcsSettingsData.DIRECTORY_PATH_PHOTOS, helper);
        saveEditTextParam(R.id.DirectoryPathVideos, RcsSettingsData.DIRECTORY_PATH_VIDEOS, helper);
        saveEditTextParam(R.id.DirectoryPathFiles, RcsSettingsData.DIRECTORY_PATH_FILES, helper);

        Spinner spinner = (Spinner) findViewById(R.id.ImSessionStart);
        if (bundle != null) {
            bundle.putInt(RcsSettingsData.IM_SESSION_START, spinner.getSelectedItemPosition());
        } else {
            mRcsSettings.writeInteger(RcsSettingsData.IM_SESSION_START,
                    spinner.getSelectedItemPosition());
        }

        saveCheckBoxParam(R.id.SmsFallbackService, RcsSettingsData.SMS_FALLBACK_SERVICE, helper);
        saveCheckBoxParam(R.id.StoreForwardServiceWarning, RcsSettingsData.WARN_SF_SERVICE, helper);
        saveCheckBoxParam(R.id.AutoAcceptFileTransfer, RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER,
                helper);
        saveCheckBoxParam(R.id.AutoAcceptChat, RcsSettingsData.AUTO_ACCEPT_CHAT, helper);
        saveCheckBoxParam(R.id.AutoAcceptGroupChat, RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT, helper);
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
