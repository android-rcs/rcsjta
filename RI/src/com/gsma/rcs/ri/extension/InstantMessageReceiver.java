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

package com.gsma.rcs.ri.extension;

import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.InstantMultimediaMessageIntent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Messaging session invitation receiver
 *
 * @author jmauffret
 */
public class InstantMessageReceiver extends BroadcastReceiver {
    private static final String LOGTAG = LogUtils.getTag(InstantMessageReceiver.class
            .getSimpleName());

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!InstantMultimediaMessageIntent.ACTION_NEW_INSTANT_MESSAGE.equals(action)) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Unknown action=" + action);
            }
            return;
        }

        /* Display instant message content */
        String content = new String(
                intent.getByteArrayExtra(InstantMultimediaMessageIntent.EXTRA_CONTENT));
        ContactId contact = intent.getParcelableExtra(InstantMultimediaMessageIntent.EXTRA_CONTACT);
        String displayName = RcsContactUtil.getInstance(context).getDisplayName(contact);
        Utils.displayLongToast(context,
                context.getString(R.string.label_recv_instant_messsage, displayName) + "\n"
                        + content);
    }
}
