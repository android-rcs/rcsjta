/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.provisioning.local;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

/**
 * A class to bind RCS settings with UI.
 * 
 * @author Philippe LEMORDANT
 */
public class ProvisioningHelper {

    private final View mRootView;
    private final RcsSettings mRcsSettings;

    public ProvisioningHelper(View view, RcsSettings rcsSettings) {
        mRootView = view;
        mRcsSettings = rcsSettings;
    }

    public void setStringEditText(int resViewTextId, String settingsKey) {
        EditText editText = (EditText) mRootView.findViewById(resViewTextId);
        editText.setText(mRcsSettings.readString(settingsKey));
    }

    public void setIntEditText(int resViewTextId, String settingsKey) {
        String value = Integer.toString(mRcsSettings.readInteger((settingsKey)));
        EditText editText = (EditText) mRootView.findViewById(resViewTextId);
        editText.setText(value);
    }

    public void setLongEditText(int resViewTextId, String settingsKey) {
        String parameter = Long.toString(mRcsSettings.readLong((settingsKey)));
        EditText editText = (EditText) mRootView.findViewById(resViewTextId);
        editText.setText(parameter);
    }

    public void setUriEditText(int resViewTextId, String settingsKey) {
        Uri dbValue = mRcsSettings.readUri(settingsKey);
        String parameter = (dbValue == null ? "" : dbValue.toString());
        EditText editText = (EditText) mRootView.findViewById(resViewTextId);
        editText.setText(parameter);
    }

    public void setBoolCheckBox(int resViewTextId, String settingsKey) {
        Boolean parameter = mRcsSettings.readBoolean((settingsKey));
        CheckBox box = (CheckBox) mRootView.findViewById(resViewTextId);
        box.setChecked(parameter);
    }

    public void setContactIdEditText(int resViewTextId, String settingsKey) {
        ContactId dbValue = mRcsSettings.readContactId(settingsKey);
        String parameter = (dbValue == null ? "" : dbValue.toString());
        EditText editText = (EditText) mRootView.findViewById(resViewTextId);
        editText.setText(parameter);
    }

    public void saveContactIdEditText(int resViewTextId, String settingsKey) {
        EditText txt = (EditText) mRootView.findViewById(resViewTextId);
        String text = txt.getText().toString();
        ContactUtil.PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(text);
        if (number == null) {
            txt.setText("");
        }
        mRcsSettings.writeContactId(settingsKey,
                "".equals(text) ? null : ContactUtil.createContactIdFromValidatedData(number));
    }

    public void saveLongEditText(int resViewTextId, String settingsKey) {
        EditText txt = (EditText) mRootView.findViewById(resViewTextId);
        mRcsSettings.writeLong(settingsKey, Long.parseLong(txt.getText().toString()));
    }

    public void saveIntEditText(int resViewTextId, String settingsKey) {
        EditText txt = (EditText) mRootView.findViewById(resViewTextId);
        mRcsSettings.writeInteger(settingsKey, Integer.parseInt(txt.getText().toString()));
    }

    public void saveStringEditText(int resViewTextId, String settingsKey) {
        EditText editText = (EditText) mRootView.findViewById(resViewTextId);
        String text = editText.getText().toString().trim();
        mRcsSettings.writeString(settingsKey, "".equals(text) ? null : text);
    }

    public void saveBoolCheckBox(int resViewTextId, String settingsKey) {
        CheckBox box = (CheckBox) mRootView.findViewById(resViewTextId);
        mRcsSettings.writeBoolean(settingsKey, box.isChecked());
    }

    public void saveUriEditText(int resViewTextId, String settingsKey) {
        EditText txt = (EditText) mRootView.findViewById(resViewTextId);
        String text = txt.getText().toString();
        mRcsSettings.writeUri(settingsKey, "".equals(text) ? null : Uri.parse(text));
    }
}
