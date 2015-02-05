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

import android.content.Context;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.orangelabs.rcs.provider.settings.RcsSettings;

/**
 * File tranfser invitation ringtone
 * 
 * @author jexa7410
 */
public class FileTransferInvitationRingtone extends RingtonePreference {

    public FileTransferInvitationRingtone(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Uri onRestoreRingtone() {
        String uri = RcsSettings.getInstance().getFileTransferInvitationRingtone();

        if (TextUtils.isEmpty(uri)) {
            return null;
        }

        return Uri.parse(uri);
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        if (ringtoneUri != null) {
            RcsSettings.getInstance().setFileTransferInvitationRingtone(ringtoneUri.toString());
        } else {
            RcsSettings.getInstance().setFileTransferInvitationRingtone("");
        }
    }
}
