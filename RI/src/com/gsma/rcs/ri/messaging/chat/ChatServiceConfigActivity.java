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

package com.gsma.rcs.ri.messaging.chat;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.Locale;

/**
 * Display/update the chat service configuration
 * 
 * @author Philippe LEMORDANT
 */
public class ChatServiceConfigActivity extends RcsActivity {

    private ChatServiceConfiguration mConfig;

    private CheckBox mRespondToDisplayReports;

    private static final String LOGTAG = LogUtils.getTag(ChatServiceConfigActivity.class
            .getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.chat_service_config);

        // Register to API connection manager
        if (!isServiceConnected(RcsServiceName.CHAT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        try {
            mConfig = getChatApi().getConfiguration();
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return;
        }
        startMonitorServices(RcsServiceName.CHAT);
        mRespondToDisplayReports = (CheckBox) findViewById(R.id.RespondToDisplayReports);
        mRespondToDisplayReports.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean enable = mRespondToDisplayReports.isChecked();
                try {
                    mConfig.setRespondToDisplayReports(enable);
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "onClick RespondToDisplayReports ".concat(enable.toString()));
                    }
                } catch (RcsServiceException e) {
                    showExceptionThenExit(e);
                }

            }

        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isExiting()) {
            return;
        }
        try {
            displayChatServiceConfig();
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void displayChatServiceConfig() throws RcsServiceException {
        Locale local = Locale.getDefault();
        CheckBox checkBox = (CheckBox) findViewById(R.id.WarnSF);
        checkBox.setChecked(mConfig.isChatWarnSF());

        TextView textView = (TextView) findViewById(R.id.IsComposingTimeout);
        textView.setText(String.format(local, "%d", mConfig.getIsComposingTimeout()));

        textView = (TextView) findViewById(R.id.MinGroupChatParticipants);
        textView.setText(String.format(local, "%d", mConfig.getGroupChatMinParticipants()));

        textView = (TextView) findViewById(R.id.MaxGroupChatParticipants);
        textView.setText(String.format(local, "%d", mConfig.getGroupChatMaxParticipants()));

        textView = (TextView) findViewById(R.id.MaxMsgLengthGroupChat);
        textView.setText(String.format(local, "%d", mConfig.getGroupChatMessageMaxLength()));

        textView = (TextView) findViewById(R.id.GroupChatSubjectMaxLength);
        textView.setText(String.format(local, "%d", mConfig.getGroupChatSubjectMaxLength()));

        textView = (TextView) findViewById(R.id.MaxMsgLengthOneToOneChat);
        textView.setText(String.format(local, "%d", mConfig.getOneToOneChatMessageMaxLength()));

        checkBox = (CheckBox) findViewById(R.id.SmsFallback);
        checkBox.setChecked(mConfig.isSmsFallback());

        mRespondToDisplayReports.setChecked(mConfig.isRespondToDisplayReportsEnabled());

        textView = (TextView) findViewById(R.id.MaxGeolocLabelLength);
        textView.setText(String.format(local, "%d", mConfig.getGeolocLabelMaxLength()));

        textView = (TextView) findViewById(R.id.GeolocExpireTime);
        textView.setText(String.format(local, "%d", mConfig.getGeolocExpirationTime()));

        checkBox = (CheckBox) findViewById(R.id.GroupChatSupported);
        checkBox.setChecked(mConfig.isGroupChatSupported());
    }
}
