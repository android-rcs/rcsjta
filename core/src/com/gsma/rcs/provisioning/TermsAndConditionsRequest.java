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
import com.gsma.rcs.provisioning.ProvisioningInfo.Version;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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

        setContentView(R.layout.rcs_terms_and_conditions);
        String title = intent.getStringExtra(EXTRA_TITLE);
        boolean accept_btn = intent.getBooleanExtra(EXTRA_ACCEPT_BTN, false);
        boolean reject_btn = intent.getBooleanExtra(EXTRA_REJECT_BTN, false);

        Button okButton = (Button) findViewById(R.id.ok_button);
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        final TextView titleText = (TextView) findViewById(R.id.title);
        final TextView messageText = (TextView) findViewById(R.id.message);

        titleText.setText(title);
        messageText.setText(message);

        /*
         * If accept and reject is enabled, then create Alert dialog with two buttons else with
         * neutral button.
         */
        if (accept_btn && reject_btn) {
            okButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    acceptTermsAndConditions();
                    finish();
                }
            });

            cancelButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    declineTermsAndConditions();
                    finish();
                }
            });

        } else {
            cancelButton.setVisibility(View.GONE);
            okButton.setText(R.string.rcs_core_terms_ok);
            okButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    acceptTermsAndConditions();
                    finish();
                }
            });
        }
    }

    private void declineTermsAndConditions() {
        Context ctx = getApplicationContext();
        ContentResolver contentResolver = ctx.getContentResolver();
        LocalContentResolver localContentResolver = new LocalContentResolver(contentResolver);
        RcsSettings rcsSettings = RcsSettings.createInstance(localContentResolver);
        MessagingLog messaginLog = MessagingLog.createInstance(localContentResolver, rcsSettings);
        ContactManager contactManager = ContactManager.createInstance(ctx, contentResolver,
                localContentResolver, rcsSettings);
        /*
         * If the user declines the terms, the RCS service is stopped and the RCS configuration is
         * reset.
         */
        LauncherUtils.stopRcsService(ctx);
        LauncherUtils.resetRcsConfig(ctx, localContentResolver, rcsSettings, messaginLog,
                contactManager);
        rcsSettings.setProvisioningVersion(Version.RESETED.toInt());
    }

    private void acceptTermsAndConditions() {
        String rcsAccountUsername = getString(R.string.rcs_core_account_username);
        Context context = getApplicationContext();
        ContentResolver contentResolver = context.getContentResolver();
        LocalContentResolver localContentResolver = new LocalContentResolver(contentResolver);
        RcsSettings rcsSettings = RcsSettings.createInstance(localContentResolver);
        ContactManager contactManager = ContactManager.createInstance(context, contentResolver,
                localContentResolver, rcsSettings);
        RcsAccountManager rcsAccountMngr = RcsAccountManager
                .createInstance(context, contactManager);

        try {
            rcsAccountMngr.createRcsAccount(rcsAccountUsername, true);
            /* Set terms and conditions accepted */
            rcsSettings.setProvisioningTermsAccepted(true);
            LauncherUtils.launchRcsCoreService(context, rcsSettings);
        } catch (RcsAccountException e) {
            sLogger.error("Failed to launch RCS service!", e);
            // TODO report an error to end user
        }
    }
}
