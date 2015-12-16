/*
 * Copyright (C) 2016 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.provisioning.https;

import static com.gsma.rcs.utils.StringUtils.UTF16;
import static com.gsma.rcs.utils.StringUtils.PDUS;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

/**
 * Handles the network initiated configuration request i.e provisioning push sms sent to port 37273.
 * IMSI-rcscfg is sent for First time configuration request and Private_User_Identity-rcscfg is sent
 * for reconfiguration request.
 */
public class ProvisioningPushSMSReceiver extends BroadcastReceiver {

    private static final Logger sLogger = Logger.getLogger(ProvisioningPushSMSReceiver.class
            .getName());

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug(new StringBuilder("Configuration SMS receiver - Received broadcast: ")
                    .append(intent.getAction()).toString());
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            if (logActivated) {
                sLogger.debug("Bundle is received with null");
            }
            return;
        }

        Object[] pdus = (Object[]) bundle.get(PDUS);
        if (pdus == null || pdus.length == 0) {
            if (logActivated) {
                sLogger.debug("Bundle contains no raw pdus");
            }
            return;
        }

        final SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[0]);
        final String smsData = new String(msg.getUserData(), UTF16);

        if (logActivated) {
            sLogger.debug("Binary SMS received with :".concat(smsData));
        }

        if (smsData.endsWith(HttpsProvisioningUtils.RESET_CONFIG_SUFFIX)) {
            final ContentResolver contentResolver = ctx.getContentResolver();
            final LocalContentResolver localContentResolver = new LocalContentResolver(
                    contentResolver);
            final RcsSettings rcsSettings = RcsSettings.getInstance(localContentResolver);
            final TelephonyManager telephonyManager = (TelephonyManager) ctx
                    .getSystemService(Context.TELEPHONY_SERVICE);

            /* Check for IMSI(fresh provisioning) or private_user_id(re-provisioning) in smsData */
            if (smsData.contains(telephonyManager.getSubscriberId())
                    || smsData.contains(rcsSettings.getUserProfileImsPrivateId())) {
                LauncherUtils.stopRcsService(ctx);
                final ContactManager contactManager = ContactManager.getInstance(ctx,
                        contentResolver, localContentResolver, rcsSettings);
                final MessagingLog messagingLog = MessagingLog.getInstance(localContentResolver,
                        rcsSettings);
                LauncherUtils.resetRcsConfig(ctx, localContentResolver, rcsSettings, messagingLog,
                        contactManager);
                LauncherUtils.launchRcsService(ctx, true, false, rcsSettings);
            }
        }
    }
}
