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

package com.orangelabs.rcs.provisioning.https;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.logger.Logger;

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
     * HttpsProvisioningManager manages http and SMS reception to load provisioning from network
     */
    HttpsProvisioningManager manager;

    /**
     * SMS provisioning receiver
     */
    private BroadcastReceiver smsProvisioningReceiver = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
     * Context
     */
	private Context context = null;


    /**
     * Constructor
     *
     * @param httpsProvisioningManager
     */
    public HttpsProvisioningSMS(HttpsProvisioningManager httpsProvisioningManager) {
        manager = httpsProvisioningManager;
        context = manager.getContext();
    }
    
    /**
     * Constructor
     *
     * @param Context
     */
    public HttpsProvisioningSMS(Context ctxt) {
        context = ctxt;
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
     * @param localContentResolver pocal content resolver
     * @param smsPort SMS port
     * @param requestUri Request URI
     * @param client Instance of {@link DefaultHttpClient}
     * @param localContext Instance of {@link HttpContext}
     */
    public void registerSmsProvisioningReceiver(final LocalContentResolver localContentResolver, final String smsPort, final String requestUri,
            final DefaultHttpClient client, final HttpContext localContext) {
        // Unregister previous one
        unregisterSmsProvisioningReceiver();

        if (logger.isActivated()) {
            logger.debug("Registering SMS provider receiver in port: " + smsPort);
        }

        // Instantiate the receiver
        smsProvisioningReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context ctx, final Intent intent) {
                if (logger.isActivated()) {
                    logger.debug("SMS provider receiver - Received broadcast: " + intent.toString());
                }

                if (HttpsProvisioningUtils.ACTION_BINARY_SMS_RECEIVED.equals(intent.getAction())) {
                    if (logger.isActivated()) {
                        logger.debug("Receiving binary SMS");
                    }

                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
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

                            if (logger.isActivated()) {
                                logger.debug("Binary SMS received with :"+smsData);
                            }
                            
                            if (logger.isActivated()) {
                                logger.debug("Binary SMS reconfiguration received");
                            }
                    		
                            if(smsData.contains(HttpsProvisioningUtils.RESET_CONFIG_SUFFIX)) {
                                if (logger.isActivated()) {
                                    logger.debug("Binary SMS reconfiguration received with suffix reconf");
                                }
                                
                                TelephonyManager tm = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
                               
                                if(!smsData.contains(tm.getSubscriberId()) && !smsData.contains(RcsSettings.getInstance().getUserProfileImsPrivateId())) {
                                	if (logger.isActivated()) {
                                        logger.debug("Binary SMS reconfiguration received but not with my ID");
                                    }
                                	return;
                                }

                                Thread t = new Thread() {
                                    public void run() {
                                    	RcsSettings.getInstance().setProvisioningVersion("0");
                                    	LauncherUtils.stopRcsService(ctx);
                                    	LauncherUtils.resetRcsConfig(ctx, localContentResolver);
                                    	LauncherUtils.launchRcsService(ctx, true, false);
                                    }
                                };
                                t.start();
                            }
                            else {
                                if (logger.isActivated()) {
                                    logger.debug("Binary SMS received for OTP");
                                }

                            	if(manager != null){
	                                Thread t = new Thread() {
	                                    public void run() {
	                                    	manager.updateConfigWithOTP(smsData, requestUri, client,
		                                                localContext);
	                                    }
	                                };
	                                t.start();
	
	                                // Unregister SMS provisioning receiver
	                                unregisterSmsProvisioningReceiver();
                            	}
                            	else
                            	{
                                    if (logger.isActivated()) {
                                        logger.warn("Binary sms received, no rcscfg requested and not waiting for OTP... Discarding SMS");
                                    }
                            	}
                            }
                            	
                        } catch (UnsupportedEncodingException e) {
                            if (logger.isActivated()) {
                                logger.debug("Parsing sms OTP failed: " + e);
                            }
                        }
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
        context.registerReceiver(smsProvisioningReceiver, intentFilter);
    }

    /**
     * Unregister the SMS provisioning receiver
     */
    public void unregisterSmsProvisioningReceiver() {
        if (smsProvisioningReceiver != null) {
            if (logger.isActivated()) {
                logger.debug("Unregistering SMS provider receiver");
            }

            try {
            	context.unregisterReceiver(smsProvisioningReceiver);
            } catch (IllegalArgumentException e) {
                // Nothing to do
            }
            smsProvisioningReceiver = null;
        }
    }
}
