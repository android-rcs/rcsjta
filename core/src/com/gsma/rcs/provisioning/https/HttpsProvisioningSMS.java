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

import com.gsma.rcs.addressbook.RcsAccountException;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provisioning.ProvisioningFailureReasons;
import com.gsma.rcs.provisioning.ProvisioningInfo.Version;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * HTTPS provisioning - SMS management
 * 
 * @author Orange
 */
public class HttpsProvisioningSMS {

    private static final String OTP_SMS_ENCODING_FORMAT = "UCS2";
    /**
     * HttpsProvisioningManager manages HTTP and SMS reception to load provisioning from network
     */
    private HttpsProvisioningManager mManager;

    /**
     * SMS provisioning receiver
     */
    private BroadcastReceiver mSmsProvisioningReceiver;

    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningSMS.class
            .getSimpleName());

    private final Context mContext;

    private final RcsSettings mRcsSettings;

    private final LocalContentResolver mLocalContentResolver;

    private final ContactManager mContactManager;

    private final MessagingLog mMessagingLog;

    /**
     * Constructor
     * 
     * @param httpsProvisioningManager HTTPs provisioning manager
     * @param localContentResolver Local content resolver
     * @param rcsSettings RCS settings accessor
     * @param messagingLog Message log accessor
     * @param contactManager Contact manager accessor
     */
    public HttpsProvisioningSMS(HttpsProvisioningManager httpsProvisioningManager, Context context,
            LocalContentResolver localContentResolver, RcsSettings rcsSettings,
            MessagingLog messagingLog, ContactManager contactManager) {
        mManager = httpsProvisioningManager;
        mContext = context;
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
     */
    public void registerSmsProvisioningReceiver(final String smsPort, final String requestUri) {
        // Unregister previous one
        unregisterSmsProvisioningReceiver();

        final boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Registering SMS provider receiver in port: ".concat(smsPort));
        }

        /* Instantiate the receiver */
        mSmsProvisioningReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context ctx, final Intent intent) {
                mManager.scheduleProvisioningOperation(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // @FIXME: Below code block needs a complete refactor, However at this
                            // moment due to other prior tasks the refactoring task has been kept in
                            // backlog.
                            if (logActivated) {
                                sLogger.debug("SMS provider receiver - Received broadcast: "
                                        .concat(intent.toString()));
                            }

                            if (!HttpsProvisioningUtils.ACTION_BINARY_SMS_RECEIVED.equals(intent
                                    .getAction())) {
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
                                System.arraycopy(data, 0, smsBufferTemp, smsBuffer.length,
                                        data.length);
                                smsBuffer = smsBufferTemp;
                            }

                            final String smsData = new String(smsBuffer, OTP_SMS_ENCODING_FORMAT);

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
                                        && !smsData.contains(mRcsSettings
                                                .getUserProfileImsPrivateId())) {
                                    if (logActivated) {
                                        sLogger.debug("Binary SMS reconfiguration received but not with my ID");
                                    }
                                    return;
                                }
                                mRcsSettings.setProvisioningVersion(Version.RESETED.toInt());
                                LauncherUtils.stopRcsService(ctx);
                                LauncherUtils.resetRcsConfig(ctx, mLocalContentResolver,
                                        mRcsSettings, mMessagingLog, mContactManager);
                                LauncherUtils.launchRcsService(ctx, true, false, mRcsSettings);
                            } else {
                                if (logActivated) {
                                    sLogger.debug("Binary SMS received for OTP");
                                }

                                if (mManager != null) {
                                    mManager.updateConfigWithOTP(smsData, requestUri);
                                    unregisterSmsProvisioningReceiver();
                                } else {
                                    if (logActivated) {
                                        sLogger.warn("Binary sms received, no rcscfg requested and not waiting for OTP... Discarding SMS");
                                    }
                                }
                            }

                        } catch (UnsupportedEncodingException e) {
                            sLogger.error(
                                    new StringBuilder("'").append(OTP_SMS_ENCODING_FORMAT)
                                            .append("'format not supported for requestUri : ")
                                            .append(requestUri).toString(), e);
                        } catch (RcsAccountException e) {
                            sLogger.error(
                                    new StringBuilder(
                                            "Failed to update Config with OTP for requestUri : ")
                                            .append(requestUri).toString(), e);
                        } catch (IOException e) {
                            if (sLogger.isActivated()) {
                                sLogger.debug(new StringBuilder(
                                        "Failed to update Config with OTP for requestUri : ")
                                        .append(requestUri).append(", Message=")
                                        .append(e.getMessage()).toString());
                            }
                            /* Start the RCS service */
                            if (mManager.isFirstProvisioningAfterBoot()) {
                                // Reason: No configuration present
                                mManager.provisioningFails(ProvisioningFailureReasons.CONNECTIVITY_ISSUE);
                                mManager.retry();
                            } else {
                                mManager.tryLaunchRcsCoreService(ctx, -1);
                            }
                        } catch (RuntimeException e) {
                            /*
                             * Normally we are not allowed to catch runtime exceptions as these are
                             * genuine bugs which should be handled/fixed within the code. However
                             * the cases when we are executing operations on a thread unhandling
                             * such exceptions will eventually lead to exit the system and thus can
                             * bring the whole system down, which is not intended.
                             */
                            sLogger.error(
                                    new StringBuilder(
                                            "Failed to update Config with OTP for requestUri : ")
                                            .append(requestUri).toString(), e);
                        }
                    }
                });
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
