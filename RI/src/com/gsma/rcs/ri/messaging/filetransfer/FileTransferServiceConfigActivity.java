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

package com.gsma.rcs.ri.messaging.filetransfer;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.filetransfer.FileTransferServiceConfiguration;
import com.gsma.services.rcs.filetransfer.FileTransferServiceConfiguration.ImageResizeOption;

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
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Locale;

/**
 * Display/update the chat service configuration
 * 
 * @author Philippe LEMORDANT
 */
public class FileTransferServiceConfigActivity extends RcsActivity {

    private FileTransferServiceConfiguration mConfig;

    private Spinner mSpinnerImageResizeOption;

    private CheckBox mCheckBoxIsAutoAccept;

    private CheckBox mCheckBoxIsAutoAcceptInRoaming;

    private static final String LOGTAG = LogUtils.getTag(FileTransferServiceConfigActivity.class
            .getSimpleName());

    private final static String[] ImageResizeOptionTab = new String[] {
            ImageResizeOption.ALWAYS_RESIZE.toString(), ImageResizeOption.ALWAYS_ASK.toString(),
            ImageResizeOption.NEVER_RESIZE.toString()
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_service_config);

        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        try {
            mConfig = getFileTransferApi().getConfiguration();
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return;
        }
        startMonitorServices(RcsServiceName.FILE_TRANSFER);

        mSpinnerImageResizeOption = (Spinner) findViewById(R.id.ft_ImageResizeOption);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
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
                    showException(e);
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
                    TableRow tableRow = (TableRow) findViewById(R.id.isAutoAcceptInRoaming);
                    if (autoAccept) {
                        tableRow.setVisibility(View.VISIBLE);
                        mCheckBoxIsAutoAcceptInRoaming.setChecked(mConfig
                                .isAutoAcceptInRoamingEnabled());
                    } else {
                        tableRow.setVisibility(View.GONE);
                    }
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "onClick isAutoAccept ".concat(autoAccept.toString()));
                    }
                } catch (RcsServiceException e) {
                    showException(e);
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
                    showException(e);
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
            displayFileTransferServiceConfig();
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private void displayFileTransferServiceConfig() throws RcsServiceException {
        Locale local = Locale.getDefault();
        TextView textView = (TextView) findViewById(R.id.ft_WarnSize);
        textView.setText(String.format(local, "%d", mConfig.getWarnSize()));

        textView = (TextView) findViewById(R.id.ft_MaxSize);
        textView.setText(String.format(local, "%d", mConfig.getMaxSize()));

        textView = (TextView) findViewById(R.id.MaxAudioDuration);
        textView.setText(String.format(local, "%d", mConfig.getMaxAudioMessageDuration()));

        if (mConfig.isAutoAcceptModeChangeable()) {
            TableRow tableRow = (TableRow) findViewById(R.id.isAutoAccept);
            tableRow.setVisibility(View.VISIBLE);
            boolean autoAcceptEnabled = mConfig.isAutoAcceptEnabled();
            mCheckBoxIsAutoAccept.setChecked(autoAcceptEnabled);
            tableRow = (TableRow) findViewById(R.id.isAutoAcceptInRoaming);
            if (autoAcceptEnabled) {
                tableRow.setVisibility(View.VISIBLE);
                mCheckBoxIsAutoAcceptInRoaming.setChecked(mConfig.isAutoAcceptInRoamingEnabled());
            } else {
                tableRow.setVisibility(View.GONE);
            }
        } else {
            TableRow tableRow = (TableRow) findViewById(R.id.isAutoAccept);
            tableRow.setVisibility(View.GONE);
            tableRow = (TableRow) findViewById(R.id.isAutoAcceptInRoaming);
            tableRow.setVisibility(View.GONE);
        }

        textView = (TextView) findViewById(R.id.MaxFileTransfers);
        textView.setText(String.format(local, "%d", mConfig.getMaxFileTransfers()));

        CheckBox checkBox = (CheckBox) findViewById(R.id.GroupFileTransferSupported); // TODO
        checkBox.setChecked(true);

        mSpinnerImageResizeOption.setSelection(mConfig.getImageResizeOption().toInt());
    }
}
