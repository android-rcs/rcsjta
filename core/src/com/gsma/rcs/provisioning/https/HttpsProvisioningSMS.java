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

package com.gsma.rcs.provisioning.https;

import static com.gsma.rcs.utils.StringUtils.PDUS;

import com.gsma.rcs.addressbook.RcsAccountException;
import com.gsma.rcs.provisioning.ProvisioningFailureReasons;
import com.gsma.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsMessage;

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
     * OTP SMS receiver
     */
    private BroadcastReceiver mOtpSmsReceiver;

    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningSMS.class.getName());

    private final Context mContext;

    /**
     * Constructor
     * 
     * @param httpsProvisioningManager HTTPs provisioning manager
     * @param context application context
     */
    public HttpsProvisioningSMS(HttpsProvisioningManager httpsProvisioningManager, Context context) {
        mManager = httpsProvisioningManager;
        mContext = context;
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
        mOtpSmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context ctx, final Intent intent) {
                mManager.scheduleProvisioningOperation(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String action = intent.getAction();
                            if (logActivated) {
                                sLogger.debug("SMS provider receiver - Received broadcast: "
                                        + action);
                            }
                            if (!HttpsProvisioningUtils.ACTION_BINARY_SMS_RECEIVED.equals(action)) {
                                return;
                            }
                            Bundle bundle = intent.getExtras();
                            if (bundle == null) {
                                return;
                            }
                            Object[] pdus = (Object[]) bundle.get(PDUS);
                            if (pdus == null || pdus.length == 0) {
                                if (logActivated) {
                                    sLogger.debug("Bundle contains no raw PDUs");
                                }
                                return;
                            }
                            if (logActivated) {
                                sLogger.debug("Receiving binary SMS");
                            }
                            SmsMessage[] msgs = new SmsMessage[pdus.length];
                            byte[] data;
                            byte[] smsBuffer = new byte[0];
                            byte[] smsBufferTemp;

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

                            mManager.updateConfigWithOTP(smsData, requestUri);
                            unregisterSmsProvisioningReceiver();

                        } catch (UnsupportedEncodingException e) {
                            sLogger.error("'" + OTP_SMS_ENCODING_FORMAT
                                    + "'format not supported for requestUri : " + requestUri, e);
                        } catch (RcsAccountException e) {
                            sLogger.error("Failed to update Config with OTP for requestUri : "
                                    + requestUri, e);
                        } catch (IOException e) {
                            if (sLogger.isActivated()) {
                                sLogger.debug("Failed to update Config with OTP for requestUri : "
                                        + requestUri + ", Message=" + e.getMessage());
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
                            sLogger.error("Failed to update Config with OTP for requestUri : "
                                    + requestUri, e);
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
        mContext.registerReceiver(mOtpSmsReceiver, intentFilter);
    }

    /**
     * Unregister the SMS provisioning receiver
     */
    public void unregisterSmsProvisioningReceiver() {
        if (mOtpSmsReceiver == null) {
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Unregistering SMS provider receiver");
        }

        try {
            mContext.unregisterReceiver(mOtpSmsReceiver);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
        mOtpSmsReceiver = null;
    }
}
