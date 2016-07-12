/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2016 Sony Mobile Communications Inc.
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

package com.gsma.rcs.provisioning.https;

import com.gsma.rcs.R;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * HTTPS provisioning - MSISDN Pop-up activity
 * 
 * @author Orange
 */
public class HttpsProvisioningAlertDialog extends Activity {

    private static final long COUNTDOWN_INTERVAL_MSEC = 1000;

    private CountDownTimer mCountDownTimer;
    private LinearLayout mErrorLayout;
    private LinearLayout mOkNormalLayout;
    private TextView mMsisdn;
    private Boolean normalCase = true;

    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningAlertDialog.class
            .getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcs_wifi_provisioning);
        setFinishOnTouchOutside(false);

        ContactId contact = getIntent().getParcelableExtra(
                HttpsProvisioningMsisdnInput.EXTRA_CONTACT);

        mErrorLayout = (LinearLayout) findViewById(R.id.error_layout);

        mOkNormalLayout = (LinearLayout) findViewById(R.id.normal_layout);
        mMsisdn = (TextView) findViewById(R.id.msisdn);

        Button okButton = (Button) findViewById(R.id.ok_button);
        okButton.setOnClickListener(mNormalButtonClickListener);

        Button cancel_button = (Button) findViewById(R.id.cancel_button);
        cancel_button.setOnClickListener(mCancelButtonClickListener);

        Button okErrorButton = (Button) findViewById(R.id.ok_error_button);
        okErrorButton.setOnClickListener(mErrorButtonClickListener);

        mCountDownTimer = new CountDownTimer(HttpsProvisioningUtils.INPUT_MSISDN_TIMEOUT,
                COUNTDOWN_INTERVAL_MSEC) {

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (sLogger.isActivated()) {
                    sLogger.warn("MSISDN dialog has expired!");
                }
                HttpsProvisioningMsisdnInput.getInstance().responseReceived(null);
                mCountDownTimer = null;
                finish();
            }
        }.start();

        if (savedInstanceState != null) {
            normalCase = savedInstanceState.getBoolean("normalCase");
        }
        if (normalCase) {
            showNormalCase();
        } else {
            showErrorCase();
        }
        if (contact != null) {
            mMsisdn.setText(contact.toString());
        }
    }

    private void showErrorCase() {
        normalCase = false;
        mErrorLayout.setVisibility(View.VISIBLE);
        mOkNormalLayout.setVisibility(View.GONE);
    }

    private void showNormalCase() {
        normalCase = true;
        mErrorLayout.setVisibility(View.GONE);
        mOkNormalLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (sLogger.isActivated()) {
            sLogger.warn("User exited the MSISDN dialog!");
        }
        HttpsProvisioningMsisdnInput.getInstance().responseReceived(null);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("normalCase", normalCase);
    }

    private OnClickListener mNormalButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String contact = mMsisdn.getText().toString();
            PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromAndroid(contact);
            if (phoneNumber == null) {
                if (sLogger.isActivated()) {
                    sLogger.warn("User entered a wrong MSISDN '" + contact + "'!");
                }
                showErrorCase();
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("User entered MSISDN ".concat(phoneNumber.getNumber()));
                }
                HttpsProvisioningMsisdnInput.getInstance().responseReceived(
                        ContactUtil.createContactIdFromValidatedData(phoneNumber));
                finish();
            }
        }
    };

    private OnClickListener mCancelButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (sLogger.isActivated()) {
                sLogger.warn("User cancelled the MSISDN dialog!");
            }
            HttpsProvisioningMsisdnInput.getInstance().responseReceived(null);
            finish();
        }
    };

    private OnClickListener mErrorButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            showNormalCase();
        }
    };

}
