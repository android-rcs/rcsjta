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

package com.gsma.rcs.provisioning.https;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

/**
 * HTTPS provisioning - SMS management
 * 
 * @author Orange
 */
public class HttpsProvisioningSMS {
    /**
     * HttpsProvisioningManager manages HTTP and SMS reception to load provisioning from network
     */
    private HttpsProvisioningManager mManager;

    /**
     * SMS provisioning receiver
     */
    private BroadcastReceiver mSmsProvisioningReceiver;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningSMS.class
            .getSimpleName());

    /**
     * Context
     */
    private final Context mContext;

    private final RcsSettings mRcsSettings;

    private final LocalContentResolver mLocalContentResolver;

    private final ContactsManager mContactManager;

    private final MessagingLog mMessagingLog;

    /**
     * Constructor
     * 
     * @param httpsProvisioningManager
     * @param localContentResolver
     * @param rcsSettings
     * @param messagingLog
     * @param contactManager
     */
    public HttpsProvisioningSMS(HttpsProvisioningManager httpsProvisioningManager,
            LocalContentResolver localContentResolver, RcsSettings rcsSettings,
            MessagingLog messagingLog, ContactsManager contactManager) {
        mManager = httpsProvisioningManager;
        mContext = mManager.getContext();
        mLocalContentResolver = localContentResolver;
        mRcsSettings = rcsSettings;
        mMessagingLog = messagingLog;
        mContactManager = contactManager;
    }

    /**
     * Generate an SMS port for provisioning
     * 
     * @return SMS port
     */
    protected static String generateSmsPortForProvisioning() {
        int minPort = 10000;
        int maxPort = 40000;
        return String.valueOf((new Random()).nextInt(maxPort - minPort) + minPort);
    }

    /**
     * Register the SMS provisioning receiver
     * 
     * @param smsPort SMS port
     * @param requestUri Request URI
     * @param client Instance of {@link DefaultHttpClient}
     * @param localContext Instance of {@link HttpContext}
     */
    public void registerSmsProvisioningReceiver(final String smsPort, final String requestUri,
            final DefaultHttpClient client, final HttpContext localContext) {
        // Unregister previous one
        unregisterSmsProvisioningReceiver();

        final boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Registering SMS provider receiver in port: ".concat(smsPort));
        }

        // Instantiate the receiver
        mSmsProvisioningReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context ctx, final Intent intent) {
                if (logActivated) {
                    sLogger.debug("SMS provider receiver - Received broadcast: ".concat(intent
                            .toString()));
                }

                if (!HttpsProvisioningUtils.ACTION_BINARY_SMS_RECEIVED.equals(intent.getAction())) {
                    return;
                }

                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    return;
                }

                if (logActivated) {
                    sLogger.debug("Receiving binary SMS");
                }

                Object[] pdus = (Object[]) bundle.get("pdus");
                SmsMessage[] msgs = new SmsMessage[pdus.length];
                byte[] data = null;
                byte[] smsBuffer = new byte[0];
                byte[] smsBufferTemp = null;

                for (int i = 0; i < msgs.length; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

                    data = msgs[i].getUserData();
                    smsBufferTemp = new byte[smsBuffer.length + data.length];
                    System.arraycopy(smsBuffer, 0, smsBufferTemp, 0, smsBuffer.length);
                    System.arraycopy(data, 0, smsBufferTemp, smsBuffer.length, data.length);
                    smsBuffer = smsBufferTemp;
                }

                try {
                    final String smsData = new String(smsBuffer, "UCS2");

                    if (logActivated) {
                        sLogger.debug("Binary SMS received with :".concat(smsData));
                    }

                    if (logActivated) {
                        sLogger.debug("Binary SMS reconfiguration received");
                    }

                    if (smsData.contains(HttpsProvisioningUtils.RESET_CONFIG_SUFFIX)) {
                        if (logActivated) {
                            sLogger.debug("Binary SMS reconfiguration received with suffix reconf");
                        }

                        TelephonyManager tm = (TelephonyManager) ctx
                                .getSystemService(Context.TELEPHONY_SERVICE);

                        if (!smsData.contains(tm.getSubscriberId())
                                && !smsData.contains(mRcsSettings.getUserProfileImsPrivateId())) {
                            if (logActivated) {
                                sLogger.debug("Binary SMS reconfiguration received but not with my ID");
                            }
                            return;
                        }

                        new Thread() {
                            public void run() {
                                mRcsSettings.setProvisioningVersion("0");
                                LauncherUtils.stopRcsService(ctx);
                                LauncherUtils.resetRcsConfig(ctx, mLocalContentResolver,
                                        mRcsSettings, mMessagingLog, mContactManager);
                                LauncherUtils.launchRcsService(ctx, true, false, mRcsSettings);
                            }
                        }.start();
                    } else {
                        if (logActivated) {
                            sLogger.debug("Binary SMS received for OTP");
                        }

                        if (mManager != null) {
                            new Thread() {
                                public void run() {
                                    mManager.updateConfigWithOTP(smsData, requestUri, client,
                                            localContext);
                                }
                            }.start();

                            // Unregister SMS provisioning receiver
                            unregisterSmsProvisioningReceiver();
                        } else {
                            if (logActivated) {
                                sLogger.warn("Binary sms received, no rcscfg requested and not waiting for OTP... Discarding SMS");
                            }
                        }
                    }

                } catch (UnsupportedEncodingException e) {
                    if (logActivated) {
                        sLogger.debug("Parsing sms OTP failed: " + e);
                    }
                }
            }
        };

        // Register receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HttpsProvisioningUtils.ACTION_BINARY_SMS_RECEIVED);
        intentFilter.addDataScheme("sms");
        intentFilter.addDataAuthority("*", smsPort);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        mContext.registerReceiver(mSmsProvisioningReceiver, intentFilter);
    }

    /**
     * Unregister the SMS provisioning receiver
     */
    public void unregisterSmsProvisioningReceiver() {
        if (mSmsProvisioningReceiver == null) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Unregistering SMS provider receiver");
        }

        try {
            mContext.unregisterReceiver(mSmsProvisioningReceiver);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
        mSmsProvisioningReceiver = null;
    }
}
