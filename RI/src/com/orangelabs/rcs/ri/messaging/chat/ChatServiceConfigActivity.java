/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
package com.orangelabs.rcs.ri.messaging.chat;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Display/update the chat service configuration
 * 
 * @author Philippe LEMORDANT
 */
public class ChatServiceConfigActivity extends Activity {

	/**
	 * API connection manager
	 */
	private ApiConnectionManager mCnxManager;

	private ChatServiceConfiguration mConfig;

	/**
	 * A locker to exit only once
	 */
	private LockAccess mExitOnce = new LockAccess();

	private CheckBox mRespondToDisplayReports;

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils
			.getTag(ChatServiceConfigActivity.class.getSimpleName());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.chat_service_config);

		// Register to API connection manager
		mCnxManager = ApiConnectionManager.getInstance(this);
		if (mCnxManager == null
				|| !mCnxManager.isServiceConnected(RcsServiceName.CHAT)) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_service_not_available), mExitOnce);
			return;
			
		}
		try {
			mConfig = mCnxManager.getChatApi().getConfiguration();
		} catch (RcsServiceException e) {
			Utils.showMessageAndExit(this,
					getString(R.string.label_api_failed), mExitOnce);
			return;
			
		}
		
		mCnxManager.startMonitorServices(this, null, RcsServiceName.CHAT);

		mRespondToDisplayReports = (CheckBox) findViewById(R.id.RespondToDisplayReports);
		mRespondToDisplayReports.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Boolean enable = mRespondToDisplayReports.isChecked();
				try {
					mConfig.setRespondToDisplayReports(enable);
					if (LogUtils.isActive) {
						Log.d(LOGTAG, "onClick RespondToDisplayReports "
								.concat(enable.toString()));
					}
				} catch (RcsServiceException e) {
					Utils.showMessageAndExit(ChatServiceConfigActivity.this,
							getString(R.string.label_api_failed), mExitOnce);
				}

			}

		});

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mCnxManager == null) {
			return;

		}
		mCnxManager.stopMonitorServices(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			displayChatServiceConfig();
		} catch (RcsServiceException e) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
		}
	}

	private void displayChatServiceConfig() throws RcsServiceException {
		CheckBox checkBox = (CheckBox) findViewById(R.id.ImAlwaysOn);
		checkBox.setChecked(mConfig.isChatSf());

		checkBox = (CheckBox) findViewById(R.id.WarnSF);
		checkBox.setChecked(mConfig.isChatWarnSF());

		TextView textView = (TextView) findViewById(R.id.IsComposingTimeout);
		textView.setText(Integer.valueOf(mConfig.getIsComposingTimeout())
				.toString());

		textView = (TextView) findViewById(R.id.ChatTimeout);
		textView.setText(Integer.valueOf(mConfig.getChatTimeout()).toString());

		textView = (TextView) findViewById(R.id.MinGroupChatParticipants);
		textView.setText(Integer.valueOf(mConfig.getGroupChatMinParticipants())
				.toString());

		textView = (TextView) findViewById(R.id.MaxGroupChatParticipants);
		textView.setText(Integer.valueOf(mConfig.getGroupChatMaxParticipants())
				.toString());

		textView = (TextView) findViewById(R.id.MaxMsgLengthGroupChat);
		textView.setText(Integer
				.valueOf(mConfig.getGroupChatMessageMaxLength()).toString());

		textView = (TextView) findViewById(R.id.GroupChatSubjectMaxLength);
		textView.setText(Integer
				.valueOf(mConfig.getGroupChatSubjectMaxLength()).toString());

		textView = (TextView) findViewById(R.id.MaxMsgLengthOneToOneChat);
		textView.setText(Integer.valueOf(
				mConfig.getOneToOneChatMessageMaxLength()).toString());

		checkBox = (CheckBox) findViewById(R.id.SmsFallback);
		checkBox.setChecked(mConfig.isSmsFallback());

		mRespondToDisplayReports.setChecked(mConfig
				.isRespondToDisplayReportsEnabled());

		textView = (TextView) findViewById(R.id.MaxGeolocLabelLength);
		textView.setText(Integer.valueOf(mConfig.getGeolocLabelMaxLength())
				.toString());

		textView = (TextView) findViewById(R.id.GeolocExpireTime);
		textView.setText(Integer.valueOf(mConfig.getGeolocExpirationTime())
				.toString());

		checkBox = (CheckBox) findViewById(R.id.GroupChatSupported);
		checkBox.setChecked(mConfig.isGroupChatSupported());
	}
}
