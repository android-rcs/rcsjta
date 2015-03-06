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

package com.orangelabs.rcs.ri.settings;

import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMethod;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.filetransfer.FileTransferServiceConfiguration.ImageResizeOption;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**
 * Messaging settings display
 *
 * @author jexa7410
 */
@SuppressWarnings("deprecation")
public class MessagingSettingsDisplay extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private CheckBoxPreference chat_displayed_notification;
    private ListPreference messagingMethod;
    private ListPreference imageResizeOption;
    private CheckBoxPreference ftAutoAccept;
    private CheckBoxPreference ftAutoAcceptInRoaming;

    private ConnectionManager mCnxManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rcs_settings_messaging_preferences);
        setTitle(R.string.rcs_settings_title_messaging_settings);

        mCnxManager = ConnectionManager.getInstance(this);
        if (mCnxManager == null
                || !mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER,
                        RcsServiceName.CHAT)) {
            Utils.showMessage(this, getString(R.string.label_service_not_available));
            return;
        }

        messagingMethod = (ListPreference) findPreference("messaging_method");
        messagingMethod.setPersistent(false);
        messagingMethod.setOnPreferenceChangeListener(this);

        chat_displayed_notification = (CheckBoxPreference) findPreference("chat_displayed_notification");
        chat_displayed_notification.setPersistent(false);
        chat_displayed_notification.setOnPreferenceChangeListener(this);

        imageResizeOption = (ListPreference) findPreference("image_resize_option");
        imageResizeOption.setPersistent(false);
        imageResizeOption.setOnPreferenceChangeListener(this);

        ftAutoAccept = (CheckBoxPreference) findPreference("ft_auto_accept");
        ftAutoAccept.setPersistent(false);
        ftAutoAccept.setOnPreferenceChangeListener(this);
        ftAutoAccept.setDisableDependentsState(false);
        ftAutoAccept.setShouldDisableView(true);

        ftAutoAcceptInRoaming = (CheckBoxPreference) findPreference("ft_auto_accept_in_roaming");
        ftAutoAcceptInRoaming.setPersistent(false);
        ftAutoAcceptInRoaming.setOnPreferenceChangeListener(this);
        ftAutoAcceptInRoaming.setDependency("ft_auto_accept");
        try {
            chat_displayed_notification.setChecked(mCnxManager.getChatApi().getConfiguration()
                    .isRespondToDisplayReportsEnabled());
            imageResizeOption.setValue(""
                    + mCnxManager.getFileTransferApi().getConfiguration().getImageResizeOption()
                            .toInt());
            ftAutoAccept.setChecked(mCnxManager.getFileTransferApi().getConfiguration()
                    .isAutoAcceptEnabled());
            ftAutoAccept.setEnabled(mCnxManager.getFileTransferApi().getConfiguration()
                    .isAutoAcceptModeChangeable());
            ftAutoAcceptInRoaming.setChecked(mCnxManager.getFileTransferApi().getConfiguration()
                    .isAutoAcceptInRoamingEnabled());
            messagingMethod.setValue(""
                    + mCnxManager.getFileTransferApi().getCommonConfiguration()
                            .getDefaultMessagingMethod().toInt());
        } catch (RcsServiceException e) {
            Utils.showMessage(this, getString(R.string.label_api_failed));
            return;
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        try {
            if (preference.getKey().equals("chat_displayed_notification")) {
                mCnxManager.getChatApi().getConfiguration()
                        .setRespondToDisplayReports((Boolean) objValue);
            } else {
                if (preference.getKey().equals("image_resize_option")) {
                    ImageResizeOption option = ImageResizeOption.valueOf(Integer
                            .parseInt((String) objValue));
                    mCnxManager.getFileTransferApi().getConfiguration()
                            .setImageResizeOption(option);
                } else {
                    if (preference.getKey().equals("ft_auto_accept")) {
                        Boolean aa = (Boolean) objValue;
                        mCnxManager.getFileTransferApi().getConfiguration().setAutoAccept(aa);
                        if (!aa) {
                            mCnxManager.getFileTransferApi().getConfiguration()
                                    .setAutoAcceptInRoaming(false);
                        }
                    } else {
                        if (preference.getKey().equals("ft_auto_accept_in_roaming")) {
                            mCnxManager.getFileTransferApi().getConfiguration()
                                    .setAutoAcceptInRoaming((Boolean) objValue);
                        } else {
                            if (preference.getKey().equals("messaging_method")) {
                                mCnxManager
                                        .getFileTransferApi()
                                        .getCommonConfiguration()
                                        .setDefaultMessagingMethod(
                                                MessagingMethod.valueOf(Integer
                                                        .parseInt((String) objValue)));
                            }
                        }
                    }
                }
            }
        } catch (RcsServiceException e) {
            Utils.showMessage(this, getString(R.string.label_api_failed));
        }
        return true;
    }
}
