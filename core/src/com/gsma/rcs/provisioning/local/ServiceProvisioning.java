/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.rcs.R;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.utils.logger.Logger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Service parameters provisioning
 * 
 * @author jexa7410
 * @author Philippe LEMORDANT
 */
public class ServiceProvisioning extends Fragment implements IProvisioningFragment {
    /**
     * IM session start modes
     */
    private static final String[] IM_SESSION_START_MODES = {
            "0", "1", "2"
    };

    private static RcsSettings sRcsSettings;
    private View mRootView;
    private static final Logger sLogger = Logger.getLogger(ServiceProvisioning.class.getName());
    private ProvisioningHelper mHelper;

    public static ServiceProvisioning newInstance(RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.debug("new instance");
        }
        ServiceProvisioning f = new ServiceProvisioning();
        /*
         * If Android decides to recreate your Fragment later, it's going to call the no-argument
         * constructor of your fragment. So overloading the constructor is not a solution. A way to
         * pass argument to new fragment is to store it as static.
         */
        sRcsSettings = rcsSettings;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.provisioning_service, container, false);
        mHelper = new ProvisioningHelper(mRootView, sRcsSettings);
        displayRcsSettings();
        return mRootView;
    }

    @Override
    public void displayRcsSettings() {
        mHelper.setLongEditText(R.id.MaxPhotoIconSize, RcsSettingsData.MAX_PHOTO_ICON_SIZE);
        mHelper.setIntEditText(R.id.MaxFreetextLength, RcsSettingsData.MAX_FREETXT_LENGTH);
        mHelper.setIntEditText(R.id.MaxChatParticipants, RcsSettingsData.MAX_CHAT_PARTICIPANTS);
        mHelper.setIntEditText(R.id.MaxChatMessageLength, RcsSettingsData.MAX_CHAT_MSG_LENGTH);
        mHelper.setIntEditText(R.id.MaxGroupChatMessageLength,
                RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH);
        mHelper.setLongEditText(R.id.ChatIdleDuration, RcsSettingsData.CHAT_IDLE_DURATION);
        mHelper.setLongEditText(R.id.MaxFileTransferSize, RcsSettingsData.MAX_FILE_TRANSFER_SIZE);
        mHelper.setLongEditText(R.id.WarnFileTransferSize, RcsSettingsData.WARN_FILE_TRANSFER_SIZE);
        mHelper.setLongEditText(R.id.MaxImageShareSize, RcsSettingsData.MAX_IMAGE_SHARE_SIZE);
        mHelper.setLongEditText(R.id.MaxVideoShareDuration,
                RcsSettingsData.MAX_VIDEO_SHARE_DURATION);
        mHelper.setLongEditText(R.id.MaxAudioMessageDuration,
                RcsSettingsData.MAX_AUDIO_MESSAGE_DURATION);
        mHelper.setIntEditText(R.id.MaxChatSessions, RcsSettingsData.MAX_CHAT_SESSIONS);
        mHelper.setIntEditText(R.id.MaxFileTransferSessions,
                RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS);
        mHelper.setIntEditText(R.id.MaxConcurrentOutgoingFileTransferSessions,
                RcsSettingsData.MAX_CONCURRENT_OUTGOING_FILE_TRANSFERS);
        mHelper.setIntEditText(R.id.MaxIpCallSessions, RcsSettingsData.MAX_IP_CALL_SESSIONS);
        mHelper.setIntEditText(R.id.MaxChatLogEntries, RcsSettingsData.MAX_CHAT_LOG_ENTRIES);
        mHelper.setIntEditText(R.id.MaxRichcallLogEntries, RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES);
        mHelper.setIntEditText(R.id.MaxIpcallLogEntries, RcsSettingsData.MAX_IPCALL_LOG_ENTRIES);
        mHelper.setStringEditText(R.id.DirectoryPathPhotos, RcsSettingsData.DIRECTORY_PATH_PHOTOS);
        mHelper.setStringEditText(R.id.DirectoryPathVideos, RcsSettingsData.DIRECTORY_PATH_VIDEOS);
        mHelper.setStringEditText(R.id.DirectoryPathAudios, RcsSettingsData.DIRECTORY_PATH_AUDIOS);
        mHelper.setStringEditText(R.id.DirectoryPathFiles, RcsSettingsData.DIRECTORY_PATH_FILES);
        mHelper.setStringEditText(R.id.DirectoryPathFileIcons,
                RcsSettingsData.DIRECTORY_PATH_FILEICONS);
        mHelper.setIntEditText(R.id.MaxGeolocLabelLength, RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH);
        mHelper.setLongEditText(R.id.GeolocExpirationTime, RcsSettingsData.GEOLOC_EXPIRATION_TIME);
        mHelper.setLongEditText(R.id.CallComposerIdleDuration,
                RcsSettingsData.CALL_COMPOSER_INACTIVITY_TIMEOUT);
        Spinner spinner = (Spinner) mRootView.findViewById(R.id.ImSessionStart);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, IM_SESSION_START_MODES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        setSpinnerParameter(spinner, RcsSettingsData.IM_SESSION_START, true, IM_SESSION_START_MODES);

        mHelper.setBoolCheckBox(R.id.SmsFallbackService, RcsSettingsData.SMS_FALLBACK_SERVICE);
        mHelper.setBoolCheckBox(R.id.StoreForwardServiceWarning, RcsSettingsData.WARN_SF_SERVICE);
        mHelper.setBoolCheckBox(R.id.AutoAcceptFileTransfer,
                RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER);
        mHelper.setBoolCheckBox(R.id.AutoAcceptFileTransferInRoaming,
                RcsSettingsData.AUTO_ACCEPT_FT_IN_ROAMING);
        mHelper.setBoolCheckBox(R.id.AutoAcceptFileTransferChangeable,
                RcsSettingsData.AUTO_ACCEPT_FT_CHANGEABLE);
        mHelper.setBoolCheckBox(R.id.AutoAcceptChat, RcsSettingsData.AUTO_ACCEPT_CHAT);
        mHelper.setBoolCheckBox(R.id.AutoAcceptGroupChat, RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT);
        mHelper.setBoolCheckBox(R.id.EnrichCalling, RcsSettingsData.ENRICH_CALLING_SERVICE);
    }

    @Override
    public void persistRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("persistRcsSettings");
        }
        mHelper.saveLongEditText(R.id.MaxPhotoIconSize, RcsSettingsData.MAX_PHOTO_ICON_SIZE);
        mHelper.saveIntEditText(R.id.MaxFreetextLength, RcsSettingsData.MAX_FREETXT_LENGTH);
        mHelper.saveIntEditText(R.id.MaxChatParticipants, RcsSettingsData.MAX_CHAT_PARTICIPANTS);
        mHelper.saveIntEditText(R.id.MaxChatMessageLength, RcsSettingsData.MAX_CHAT_MSG_LENGTH);
        mHelper.saveIntEditText(R.id.MaxGroupChatMessageLength,
                RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH);
        mHelper.saveLongEditText(R.id.ChatIdleDuration, RcsSettingsData.CHAT_IDLE_DURATION);
        mHelper.saveLongEditText(R.id.MaxFileTransferSize, RcsSettingsData.MAX_FILE_TRANSFER_SIZE);
        mHelper.saveLongEditText(R.id.WarnFileTransferSize, RcsSettingsData.WARN_FILE_TRANSFER_SIZE);
        mHelper.saveLongEditText(R.id.MaxImageShareSize, RcsSettingsData.MAX_IMAGE_SHARE_SIZE);
        mHelper.saveLongEditText(R.id.MaxVideoShareDuration,
                RcsSettingsData.MAX_VIDEO_SHARE_DURATION);
        mHelper.saveLongEditText(R.id.MaxAudioMessageDuration,
                RcsSettingsData.MAX_AUDIO_MESSAGE_DURATION);
        mHelper.saveIntEditText(R.id.MaxChatSessions, RcsSettingsData.MAX_CHAT_SESSIONS);
        mHelper.saveIntEditText(R.id.MaxFileTransferSessions,
                RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS);
        mHelper.saveIntEditText(R.id.MaxConcurrentOutgoingFileTransferSessions,
                RcsSettingsData.MAX_CONCURRENT_OUTGOING_FILE_TRANSFERS);
        mHelper.saveIntEditText(R.id.MaxIpCallSessions, RcsSettingsData.MAX_IP_CALL_SESSIONS);
        mHelper.saveIntEditText(R.id.MaxChatLogEntries, RcsSettingsData.MAX_CHAT_LOG_ENTRIES);
        mHelper.saveIntEditText(R.id.MaxRichcallLogEntries,
                RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES);
        mHelper.saveIntEditText(R.id.MaxIpcallLogEntries, RcsSettingsData.MAX_IPCALL_LOG_ENTRIES);
        mHelper.saveIntEditText(R.id.MaxGeolocLabelLength, RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH);
        mHelper.saveLongEditText(R.id.GeolocExpirationTime, RcsSettingsData.GEOLOC_EXPIRATION_TIME);
        mHelper.saveStringEditText(R.id.DirectoryPathPhotos, RcsSettingsData.DIRECTORY_PATH_PHOTOS);
        mHelper.saveStringEditText(R.id.DirectoryPathVideos, RcsSettingsData.DIRECTORY_PATH_VIDEOS);
        mHelper.saveStringEditText(R.id.DirectoryPathAudios, RcsSettingsData.DIRECTORY_PATH_AUDIOS);
        mHelper.saveStringEditText(R.id.DirectoryPathFiles, RcsSettingsData.DIRECTORY_PATH_FILES);
        mHelper.saveLongEditText(R.id.CallComposerIdleDuration,
                RcsSettingsData.CALL_COMPOSER_INACTIVITY_TIMEOUT);
        mHelper.saveStringEditText(R.id.DirectoryPathFileIcons,
                RcsSettingsData.DIRECTORY_PATH_FILEICONS);

        Spinner spinner = (Spinner) mRootView.findViewById(R.id.ImSessionStart);
        sRcsSettings.writeInteger(RcsSettingsData.IM_SESSION_START,
                spinner.getSelectedItemPosition());

        mHelper.saveBoolCheckBox(R.id.SmsFallbackService, RcsSettingsData.SMS_FALLBACK_SERVICE);
        mHelper.saveBoolCheckBox(R.id.StoreForwardServiceWarning, RcsSettingsData.WARN_SF_SERVICE);
        mHelper.saveBoolCheckBox(R.id.AutoAcceptFileTransfer,
                RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER);
        mHelper.saveBoolCheckBox(R.id.AutoAcceptFileTransferInRoaming,
                RcsSettingsData.AUTO_ACCEPT_FT_IN_ROAMING);
        mHelper.saveBoolCheckBox(R.id.AutoAcceptFileTransferChangeable,
                RcsSettingsData.AUTO_ACCEPT_FT_CHANGEABLE);
        mHelper.saveBoolCheckBox(R.id.AutoAcceptChat, RcsSettingsData.AUTO_ACCEPT_CHAT);
        mHelper.saveBoolCheckBox(R.id.AutoAcceptGroupChat, RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT);
        mHelper.saveBoolCheckBox(R.id.EnrichCalling, RcsSettingsData.ENRICH_CALLING_SERVICE);
    }

    int setSpinnerParameter(final Spinner spinner, String settingsKey, boolean isSettingInteger,
            final String[] selection) {
        Integer parameter;
        if (isSettingInteger) {
            parameter = sRcsSettings.readInteger(settingsKey);
        } else {
            String selected = sRcsSettings.readString(settingsKey);
            parameter = java.util.Arrays.asList(selection).indexOf(selected);
        }
        spinner.setSelection(parameter % selection.length);
        return parameter;
    }

}
