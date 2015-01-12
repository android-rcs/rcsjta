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
package com.orangelabs.rcs.ri.messaging.ft;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.ft.FileTransferServiceConfiguration;
import com.gsma.services.rcs.ft.FileTransferServiceConfiguration.ImageResizeOption;
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
public class FileTransferServiceConfigActivity extends Activity {

	/**
	 * API connection manager
	 */
	private ApiConnectionManager mCnxManager;

	private FileTransferServiceConfiguration mConfig;

	private Spinner mSpinnerImageResizeOption;

	private CheckBox mCheckBoxIsAutoAccept;

	private CheckBox mCheckBoxIsAutoAcceptInRoaming;

	/**
	 * A locker to exit only once
	 */
	private LockAccess mExitOnce = new LockAccess();

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(FileTransferServiceConfigActivity.class
			.getSimpleName());

	private final static String[] ImageResizeOptionTab = new String[] {
			ImageResizeOption.ALWAYS_PERFORM.toString(),
			ImageResizeOption.ONLY_ABOVE_MAX_SIZE.toString(), ImageResizeOption.ASK.toString() };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.filetransfer_service_config);

		// Register to API connection manager
		mCnxManager = ApiConnectionManager.getInstance(this);
		if (mCnxManager == null || !mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
					mExitOnce);
			return;

		}
		try {
			mConfig = mCnxManager.getFileTransferApi().getConfiguration();
		} catch (RcsServiceException e) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
			return;

		}

		mCnxManager.startMonitorServices(this, null, RcsServiceName.FILE_TRANSFER);

		mSpinnerImageResizeOption = (Spinner) findViewById(R.id.ft_ImageResizeOption);
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, ImageResizeOptionTab);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerImageResizeOption.setAdapter(dataAdapter);
		mSpinnerImageResizeOption.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
					int position, long id) {
				ImageResizeOption newOption = ImageResizeOption.valueOf(mSpinnerImageResizeOption
						.getSelectedItemPosition());
				try {
					ImageResizeOption oldOption = mConfig.getImageResizeOption();
					if (!oldOption.equals(newOption)) {
						mConfig.setImageResizeOption(newOption);
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onClick ImageResizeOption".concat(newOption.toString()));

						}
					}
				} catch (RcsServiceException e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Exception occurred", e);
					}
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}
		});

		mCheckBoxIsAutoAccept = (CheckBox) findViewById(R.id.ft_isAutoAccept);
		mCheckBoxIsAutoAccept.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Boolean autoAccept = mCheckBoxIsAutoAccept.isChecked();
				try {
					mConfig.setAutoAccept(autoAccept);
					if (LogUtils.isActive) {
						Log.d(LOGTAG, "onClick isAutoAccept ".concat(autoAccept.toString()));
					}
				} catch (RcsServiceException e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Exception occurred", e);
					}
				}

			}

		});

		mCheckBoxIsAutoAcceptInRoaming = (CheckBox) findViewById(R.id.ft_isAutoAcceptInRoaming);
		mCheckBoxIsAutoAcceptInRoaming.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Boolean autoAcceptInRoaming = mCheckBoxIsAutoAcceptInRoaming.isChecked();
				try {
					mConfig.setAutoAcceptInRoaming(autoAcceptInRoaming);
					if (LogUtils.isActive) {
						Log.d(LOGTAG, "onClick isAutoAcceptInRoaming ".concat(autoAcceptInRoaming
								.toString()));
					}
				} catch (RcsServiceException e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Exception occurred", e);
					}
					Utils.showMessageAndExit(FileTransferServiceConfigActivity.this,
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
			displayFileTransferServiceConfig();
		} catch (RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
		}
	}

	private void displayFileTransferServiceConfig() throws RcsServiceException {
		TextView textView = (TextView) findViewById(R.id.ft_WarnSize);
		textView.setText(Long.valueOf(mConfig.getWarnSize()).toString());

		textView = (TextView) findViewById(R.id.ft_MaxSize);
		textView.setText(Long.valueOf(mConfig.getMaxSize()).toString());

		mCheckBoxIsAutoAccept.setChecked(mConfig.isAutoAcceptEnabled());

		CheckBox checkBox = (CheckBox) findViewById(R.id.ft_isAutoAcceptModeChangeable);
		checkBox.setChecked(mConfig.isAutoAcceptModeChangeable());

		mCheckBoxIsAutoAcceptInRoaming.setChecked(mConfig.isAutoAcceptInRoamingEnabled());

		textView = (TextView) findViewById(R.id.MaxFileTransfers);
		textView.setText(Integer.valueOf(mConfig.getMaxFileTransfers()).toString());

		checkBox = (CheckBox) findViewById(R.id.GroupFileTransferSupported); // TODO
		checkBox.setChecked(true);

		mSpinnerImageResizeOption.setSelection(mConfig.getImageResizeOption().toInt());
	}
}
