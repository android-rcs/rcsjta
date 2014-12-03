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

package com.orangelabs.rcs.provisioning;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.widget.TextView;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.LocalContentResolver;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.LauncherUtils;

/**
 * Show the request for terms and conditions
 *
 * @author hlxn7157
 */
public class TermsAndConditionsRequest extends Activity {

    /**
     * Intent keys
     */
    public static final String MESSAGE_KEY = "message";
    public static final String TITLE_KEY = "title";
    public static final String ACCEPT_BTN_KEY = "accept_btn";
    public static final String REJECT_BTN_KEY = "reject_btn";

    private LocalContentResolver mLocalContentResolver;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        final Context ctx = getApplicationContext();
        mLocalContentResolver = new LocalContentResolver(ctx.getContentResolver());
        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra(TITLE_KEY);
            String message = intent.getStringExtra(MESSAGE_KEY);
            boolean accept_btn = intent.getBooleanExtra(ACCEPT_BTN_KEY, false);
            boolean reject_btn = intent.getBooleanExtra(REJECT_BTN_KEY, false);
            
            if (!TextUtils.isEmpty(message)) {
                // Add text
                TextView textView = new TextView(this);
                textView.setAutoLinkMask(Linkify.ALL);
                textView.setText(message);
                textView.setPadding(10, 10, 10, 10);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(title).setView(textView);

                // If accept and reject is enabled, then create Alert dialog
                // with two buttons else with neutral button
                if (accept_btn && reject_btn) {
                    builder.setPositiveButton(R.string.rcs_core_terms_accept,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Set terms and conditions accepted
                                    RcsSettings.createInstance(ctx);
                                    RcsSettings.getInstance().setProvisioningTermsAccepted(true);
                                    finish();
                                }
                            });

                    builder.setNegativeButton(R.string.rcs_core_terms_decline,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // If the user declines the terms, the RCS service is stopped
                                	// and the RCS config is reset
                                    LauncherUtils.stopRcsService(ctx);
                                    LauncherUtils.resetRcsConfig(ctx, mLocalContentResolver);
                                    RcsSettings.getInstance().setProvisioningVersion("0");
                                    finish();
                                }
                            });
                } else {
                    builder.setNeutralButton(R.string.rcs_core_terms_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Set terms and conditions accepted
                                    RcsSettings.createInstance(getApplicationContext());
                                    RcsSettings.getInstance().setProvisioningTermsAccepted(true);
                                    finish();
                                }
                            });
                }

                AlertDialog alert = builder.create(); 
                alert.setCanceledOnTouchOutside(false);
                alert.setCancelable(false);
                alert.show();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }
}
