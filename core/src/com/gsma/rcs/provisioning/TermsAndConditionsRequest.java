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
import com.gsma.rcs.provider.settings.RcsSettingsData.TermsAndConditionsResponse;
import com.gsma.rcs.provisioning.ProvisioningInfo.Version;
import com.gsma.rcs.service.LauncherUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

    private static final Logger sLogger = Logger.getLogger(TermsAndConditionsRequest.class
            .getSimpleName());

    private static final int TC_NOTIFICATION_ID = 1;
    private static final String TC_NOTIFICATION_TAG = TermsAndConditionsRequest.class
            .getSimpleName();

    private String mMessage;

    private String mTitle;

    private RcsSettings mRcsSettings;

    private LocalContentResolver mLocalContentResolver;

    private ContactManager mContactManager;

    private MessagingLog mMessaginLog;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        mContext = getApplicationContext();
        ContentResolver contentResolver = mContext.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(contentResolver);
        mRcsSettings = RcsSettings.getInstance(mLocalContentResolver);
        mMessaginLog = MessagingLog.getInstance(mLocalContentResolver, mRcsSettings);
        mContactManager = ContactManager.getInstance(mContext, contentResolver,
                mLocalContentResolver, mRcsSettings);

        setContentView(R.layout.rcs_terms_and_conditions);
        setFinishOnTouchOutside(false);
        mMessage = mRcsSettings.getProvisioningUserMessageContent();
        mTitle = mRcsSettings.getProvisioningUserMessageTitle();
        boolean accept_btn = mRcsSettings.isProvisioningAcceptButton();
        boolean reject_btn = mRcsSettings.isProvisioningRejectButton();

        Button okButton = (Button) findViewById(R.id.ok_button);
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        TextView titleText = (TextView) findViewById(R.id.title);
        TextView messageText = (TextView) findViewById(R.id.message);

        if (mTitle != null) {
            titleText.setText(mTitle);
        }
        if (mMessage != null) {
            messageText.setText(mMessage);
        }

        /*
         * If both accept and reject are enabled then creates alert dialog with two buttons else
         * with neutral button.
         */
        if (accept_btn && reject_btn) {
            okButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    acceptTermsAndConditions();
                }
            });

            cancelButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    declineTermsAndConditions();
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

    @Override
    protected void onResume() {
        cancelTermsAndConditionsNotification(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        if (!isFinishing() && !isChangingConfigurations()) {
            if (sLogger.isActivated()) {
                sLogger.info("Terms and conditions windows closed");
            }
            showTermsAndConditionsNotification();
        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (sLogger.isActivated()) {
            sLogger.info("User discards terms and conditions");
        }
        showTermsAndConditionsNotification();
        super.onBackPressed();
    }

    private void declineTermsAndConditions() {
        if (sLogger.isActivated()) {
            sLogger.info("User declines terms and conditions");
        }
        /*
         * If the user declines the terms or does not answer, the RCS service is stopped and the RCS
         * configuration is reset.
         */
        LauncherUtils.stopRcsService(mContext);
        LauncherUtils.resetRcsConfig(mContext, mLocalContentResolver, mRcsSettings, mMessaginLog,
                mContactManager);
        mRcsSettings.setTermsAndConditionsResponse(TermsAndConditionsResponse.DECLINED);
        mRcsSettings.setProvisioningVersion(Version.RESETED.toInt());
        finish();
    }

    private void acceptTermsAndConditions() {
        if (sLogger.isActivated()) {
            sLogger.info("User accepts terms and conditions");
        }
        String rcsAccountUsername = getString(R.string.rcs_core_account_username);
        RcsAccountManager rcsAccountMngr = RcsAccountManager.getInstance(mContext,
                mContactManager);
        try {
            rcsAccountMngr.createRcsAccount(rcsAccountUsername, true);
            /* Set terms and conditions accepted */
            mRcsSettings.setTermsAndConditionsResponse(TermsAndConditionsResponse.ACCEPTED);
            /* We accept a configuration which was successfully parsed */
            mRcsSettings.setConfigurationValid(true);
            LauncherUtils.launchRcsCoreService(mContext, mRcsSettings);
        } catch (RcsAccountException e) {
            sLogger.error("Failed to launch RCS service!", e);
            // TODO report an error to end user
        }
        finish();
    }

    private void showTermsAndConditionsNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0, getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT);

        /* Create notification */
        Notification.Builder notif = new Notification.Builder(this);
        notif.setContentIntent(pi);
        notif.setSmallIcon(R.drawable.rcs_notif_on_icon);
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(true);
        notif.setOnlyAlertOnce(true);
        notif.setContentTitle(getString(R.string.rcs_core_terms_title));
        notif.setContentText(mTitle);
        notif.setDefaults(Notification.DEFAULT_VIBRATE);

        /* Send notification */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(TC_NOTIFICATION_TAG, TC_NOTIFICATION_ID, notif.build());
    }

    /**
     * Cancel the terms and conditions request notification
     * 
     * @param context The context
     */
    public static void cancelTermsAndConditionsNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(TC_NOTIFICATION_TAG, TC_NOTIFICATION_ID);
    }

    /**
     * Show the terms and conditions request
     * 
     * @param context The context
     */
    public static void showTermsAndConditions(Context context) {
        final Intent intent = new Intent(context, TermsAndConditionsRequest.class);
        /* Required as the activity is started outside of an Activity context */
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }
}
