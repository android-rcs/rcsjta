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

package com.gsma.rcs.provisioning.https;

import com.gsma.rcs.R;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * HTTPS provisioning - MSISDN Pop-up activity
 * 
 * @author Orange
 */
public class HttpsProvisioningAlertDialog extends Activity {

    private static final long COUNTDOWN_INTERVAL_MSEC = 1000;
    
    private AlertDialog mErrorDialog;
    
    private CountDownTimer mCountDownTimer;
    
    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningAlertDialog.class.getSimpleName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rcs_wifi_provisioning);

        Button ok_button = (Button) findViewById(R.id.ok_button);

        final TextView msisdn = (TextView) findViewById(R.id.msisdn);

        ok_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String contact = msisdn.getText().toString();
                PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromAndroid(contact);
                if (phoneNumber == null) {
                    if (sLogger.isActivated())  {
                        sLogger.warn("User entered a wrong MSISDN '"+contact+"'!");
                    }
                    mErrorDialog = new AlertDialog.Builder(HttpsProvisioningAlertDialog.this)
                            .setTitle(R.string.label_invalid_phone_number)
                            .setPositiveButton(R.string.label_ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            mErrorDialog.dismiss();
                                            mErrorDialog = null;
                                        }
                                    }).create();
                    mErrorDialog.setCanceledOnTouchOutside(false);
                    mErrorDialog.show();
                } else {
                    if (sLogger.isActivated())  {
                        sLogger.debug("User entered MSISDN ".concat(phoneNumber.getNumber()));
                    }
                    HttpsProvionningMSISDNInput.getInstance().responseReceived(
                            ContactUtil.createContactIdFromValidatedData(phoneNumber));
                    finish();
                }
            }
        });

        Button cancel_button = (Button) findViewById(R.id.cancel_button);
        cancel_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sLogger.isActivated())  {
                    sLogger.warn("User cancelled the MSISDN dialog!");
                }
                HttpsProvionningMSISDNInput.getInstance().responseReceived(null);
                finish();
            }
        });
        mCountDownTimer = new CountDownTimer(HttpsProvisioningUtils.INPUT_MSISDN_TIMEOUT, COUNTDOWN_INTERVAL_MSEC) {

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (sLogger.isActivated())  {
                    sLogger.warn("MSISDN dialog has expired!");
                }
                HttpsProvionningMSISDNInput.getInstance().responseReceived(null);
                mCountDownTimer = null;
                finish();
            }
        }.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (sLogger.isActivated())  {
                    sLogger.warn("User exited the MSISDN dialog!");
                }
                HttpsProvionningMSISDNInput.getInstance().responseReceived(null);
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (mErrorDialog != null) {
            mErrorDialog.dismiss();
        }
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        super.onDestroy();
    }

}
