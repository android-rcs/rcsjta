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

package com.orangelabs.rcs.settings;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.gsma.services.rcs.ft.FileTransferServiceConfiguration.ImageResizeOption;
import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Messaging settings display
 *
 * @author jexa7410
 */
public class MessagingSettingsDisplay extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private CheckBoxPreference filetransfer_vibrate;
    private CheckBoxPreference chat_vibrate;
    private CheckBoxPreference chat_displayed_notification;
    private ListPreference imageResizeOption;
    private CheckBoxPreference ftAutoAccept;
    private CheckBoxPreference ftAutoAcceptInRoaming;
    
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(MessagingSettingsDisplay.class.getSimpleName());
        
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.rcs_settings_messaging_preferences);
        setTitle(R.string.rcs_settings_title_messaging_settings);
        
        filetransfer_vibrate = (CheckBoxPreference)findPreference("filetransfer_invitation_vibration");
        filetransfer_vibrate.setPersistent(false);
        filetransfer_vibrate.setOnPreferenceChangeListener(this);
        filetransfer_vibrate.setChecked(RcsSettings.getInstance().isPhoneVibrateForFileTransferInvitation());

        chat_vibrate = (CheckBoxPreference)findPreference("chat_invitation_vibration");
        chat_vibrate.setPersistent(false);
        chat_vibrate.setOnPreferenceChangeListener(this);
        chat_vibrate.setChecked(RcsSettings.getInstance().isPhoneVibrateForChatInvitation());

        chat_displayed_notification = (CheckBoxPreference)findPreference("chat_displayed_notification");
        chat_displayed_notification.setPersistent(false);
        chat_displayed_notification.setOnPreferenceChangeListener(this);
        chat_displayed_notification.setChecked(RcsSettings.getInstance().isRespondToDisplayReports());
        
        imageResizeOption = (ListPreference) findPreference("image_resize_option");
        imageResizeOption.setPersistent(false);
        imageResizeOption.setOnPreferenceChangeListener(this);
        imageResizeOption.setValue("" + RcsSettings.getInstance().getImageResizeOption().toInt());
               
        ftAutoAccept = (CheckBoxPreference)findPreference("ft_auto_accept");
        ftAutoAccept.setPersistent(false);
        ftAutoAccept.setOnPreferenceChangeListener(this);
        ftAutoAccept.setChecked(RcsSettings.getInstance().isFileTransferAutoAccepted());
        ftAutoAccept.setDisableDependentsState(false);
        ftAutoAccept.setEnabled(RcsSettings.getInstance().isFtAutoAcceptedModeChangeable());
        ftAutoAccept.setShouldDisableView(true);
        
        ftAutoAcceptInRoaming = (CheckBoxPreference)findPreference("ft_auto_accept_in_roaming");
        ftAutoAcceptInRoaming.setPersistent(false);
        ftAutoAcceptInRoaming.setOnPreferenceChangeListener(this);
        ftAutoAcceptInRoaming.setChecked(RcsSettings.getInstance().isFileTransferAutoAcceptedInRoaming());
        ftAutoAcceptInRoaming.setDependency("ft_auto_accept");
	}

	public boolean onPreferenceChange(Preference preference, Object objValue) {
		RcsSettings rcsSettings = RcsSettings.getInstance();
		if (preference.getKey().equals("filetransfer_invitation_vibration")) {
			rcsSettings.setPhoneVibrateForFileTransferInvitation((Boolean) objValue);
		} else {
			if (preference.getKey().equals("chat_invitation_vibration")) {
				rcsSettings.setPhoneVibrateForChatInvitation((Boolean) objValue);
			} else {
				if (preference.getKey().equals("chat_displayed_notification")) {
					rcsSettings.setRespondToDisplayReports((Boolean) objValue);
				} else {
					if (preference.getKey().equals("image_resize_option")) {
						try {
							ImageResizeOption option = ImageResizeOption.valueOf(Integer.parseInt((String) objValue));
							// Set the image resize option
							rcsSettings.setImageResizeOption(option);
						} catch (Exception e) {
							if (logger.isActivated()) {
								logger.warn("Invalid image resize option: "+objValue);
							}
						}

					} else {
						if (preference.getKey().equals("ft_auto_accept")) {
							Boolean aa = (Boolean) objValue;
							rcsSettings.setFileTransferAutoAccepted(aa);
							if (!aa) {
								rcsSettings.setFileTransferAutoAcceptedInRoaming(false);
							}
						} else {
							if (preference.getKey().equals("ft_auto_accept_in_roaming")) {
								rcsSettings.setFileTransferAutoAcceptedInRoaming((Boolean) objValue);
							}
						}
					}
				}
			}
		}
		return true;
	}
}
