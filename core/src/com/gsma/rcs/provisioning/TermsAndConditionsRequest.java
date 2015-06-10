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

package com.gsma.rcs.provisioning;

import com.gsma.rcs.R;
import com.gsma.rcs.addressbook.RcsAccountException;
import com.gsma.rcs.addressbook.RcsAccountManager;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.widget.TextView;

/**
 * Show the request for terms and conditions
 * 
 * @author hlxn7157
 */
public class TermsAndConditionsRequest extends Activity {

    /**
     * Intent extra: message
     */
    public static final String EXTRA_MESSAGE = "message";
    /**
     * Intent extra: title
     */
    public static final String EXTRA_TITLE = "title";
    /**
     * Intent extra: accept button
     */
    public static final String EXTRA_ACCEPT_BTN = "accept_btn";
    /**
     * Intent extra: reject button
     */
    public static final String EXTRA_REJECT_BTN = "reject_btn";

    private static final Logger sLogger = Logger.getLogger(TermsAndConditionsRequest.class
            .getName());

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        final Context ctx = getApplicationContext();
        final ContentResolver contentResolver = ctx.getContentResolver();
        final LocalContentResolver localContentResolver = new LocalContentResolver(contentResolver);
        final RcsSettings rcsSettings = RcsSettings.createInstance(localContentResolver);
        final MessagingLog messaginLog = MessagingLog.createInstance(localContentResolver,
                rcsSettings);
        final ContactManager contactManager = ContactManager.createInstance(ctx, contentResolver,
                localContentResolver, rcsSettings);
        final RcsAccountManager rcsAccountMngr = RcsAccountManager.createInstance(ctx,
                contactManager);
        final String rcsAccountUsername = getString(R.string.rcs_core_account_username);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        if (TextUtils.isEmpty(message)) {
            finish();
            return;
        }
        String title = intent.getStringExtra(EXTRA_TITLE);
        boolean accept_btn = intent.getBooleanExtra(EXTRA_ACCEPT_BTN, false);
        boolean reject_btn = intent.getBooleanExtra(EXTRA_REJECT_BTN, false);

        /* Add text */
        TextView textView = new TextView(this);
        textView.setAutoLinkMask(Linkify.ALL);
        textView.setText(message);
        textView.setPadding(10, 10, 10, 10);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setView(textView);

        /*
         * If accept and reject is enabled, then create Alert dialog with two buttons else with
         * neutral button.
         */
        if (accept_btn && reject_btn) {
            builder.setPositiveButton(R.string.rcs_core_terms_accept,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                rcsAccountMngr.createRcsAccount(rcsAccountUsername, true);
                                /* Set terms and conditions accepted */
                                rcsSettings.setProvisioningTermsAccepted(true);
                                LauncherUtils.launchRcsCoreService(ctx, rcsSettings);
                            } catch (RcsAccountException e) {
                                sLogger.error("Failed to launch RCS service!", e);
                                // TODO report an error to end user
                            }
                            finish();
                        }
                    });

            builder.setNegativeButton(R.string.rcs_core_terms_decline,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            /*
                             * If the user declines the terms, the RCS service is stopped and the
                             * RCS configuration is reset.
                             */
                            LauncherUtils.stopRcsService(ctx);
                            LauncherUtils.resetRcsConfig(ctx, localContentResolver, rcsSettings,
                                    messaginLog, contactManager);
                            rcsSettings.setProvisioningVersion("0");
                            finish();
                        }
                    });
        } else {
            builder.setNeutralButton(R.string.rcs_core_terms_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                rcsAccountMngr.createRcsAccount(rcsAccountUsername, true);
                                /* Set terms and conditions accepted */
                                rcsSettings.setProvisioningTermsAccepted(true);
                                LauncherUtils.launchRcsCoreService(ctx, rcsSettings);
                            } catch (RcsAccountException e) {
                                sLogger.error("Failed to launch RCS service!", e);
                                // TODO report an error to end user
                            }
                            finish();
                        }
                    });
        }

        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.setCancelable(false);
        alert.show();
    }
}
