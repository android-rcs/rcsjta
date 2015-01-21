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

package com.orangelabs.rcs.ri.service;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.gsma.services.rcs.CommonServiceConfiguration;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMethod;
import com.gsma.services.rcs.RcsServiceException;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * @author yplo6403
 *
 */
public class CommonServiceConfigurationActivity extends Activity {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(CommonServiceConfigurationActivity.class
			.getSimpleName());

	private static final String[] DEF_MSG_METHOD = new String[] { MessagingMethod.AUTOMATIC.name(),
			MessagingMethod.RCS.name(), MessagingMethod.NON_RCS.name() };

	/**
	 * API connection manager
	 */
	private ApiConnectionManager mCnxManager;

	/**
	 * A locker to exit only once
	 */
	private LockAccess mExitOnce = new LockAccess();

	private Spinner mSpinnerDefMessaginMethod;
	private TextView mTextEditDisplayName;
	private CheckBox mCheckBoxIsConfigValid;
	private TextView mTextEditMessagingUX;
	private TextView mTextEditContactId;

	private CommonServiceConfiguration mConfiguration;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.service_configuration);

		// Register to API connection manager
		mCnxManager = ApiConnectionManager.getInstance(this);
		if (mCnxManager == null || !mCnxManager.isServiceConnected(RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
					mExitOnce);
			return;

		}
		try {
			mConfiguration = mCnxManager.getContactsApi().getCommonConfiguration();
		} catch (RcsServiceException e1) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
			return;

		}

		mCheckBoxIsConfigValid = (CheckBox) findViewById(R.id.label_service_configuration_valid);
		mTextEditMessagingUX = (TextView) findViewById(R.id.label_messaging_mode);
		mTextEditContactId = (TextView) findViewById(R.id.label_my_contact_id);

		mTextEditDisplayName = (TextView) findViewById(R.id.text_my_display_name);
		final Button btnDisplayName = (Button) findViewById(R.id.button_display_name);
		btnDisplayName.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String newDisplayName = mTextEditDisplayName.getText().toString();
				setDisplayName(newDisplayName);
			}

		});

		mSpinnerDefMessaginMethod = (Spinner) findViewById(R.id.spinner_default_messaging_method);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, DEF_MSG_METHOD);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerDefMessaginMethod.setAdapter(adapter);

		mSpinnerDefMessaginMethod.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
					int position, long id) {
				MessagingMethod method = MessagingMethod.valueOf(mSpinnerDefMessaginMethod
						.getSelectedItemPosition());
				try {
					MessagingMethod oldMethod = mConfiguration.getDefaultMessagingMethod();
					if (!oldMethod.equals(method)) {
						mConfiguration.setDefaultMessagingMethod(method);
						if (LogUtils.isActive) {
							Log.d(LOGTAG,
									"onClick DefaultMessagingMethod ".concat(method.toString()));
						}
					}
				} catch (RcsServiceException e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Exception occurred", e);
					}
					Utils.showMessageAndExit(CommonServiceConfigurationActivity.this,
							getString(R.string.label_api_failed), mExitOnce);
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}
		});

		mCnxManager.startMonitorServices(this, null, RcsServiceName.CONTACTS);

		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreate");
		}
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
		displayServiceConfiguration();
	}

	private void displayServiceConfiguration() {
		try {
			mCheckBoxIsConfigValid.setChecked(mConfiguration.isConfigValid());
			mSpinnerDefMessaginMethod.setSelection(mConfiguration.getDefaultMessagingMethod()
					.toInt());
			mTextEditMessagingUX.setText(mConfiguration.getMessagingUX().name());
			mTextEditDisplayName.setText(mConfiguration.getMyDisplayName());
			mTextEditContactId.setText(mConfiguration.getMyContactId().toString());
		} catch (RcsServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Exception occurred", e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
		}
	}

	private void setDisplayName(String newDisplayName) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "Refresh display name to ".concat(newDisplayName));
		}
		try {
			mConfiguration.setMyDisplayName(newDisplayName);
		} catch (RcsServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Exception occurred", e);
			}
		}
	}

}
