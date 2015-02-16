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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.EditText;

import java.lang.ref.WeakReference;

/**
 * HTTPS provisioning - MSISDN Pop-up activity
 * 
 * @author Orange
 */
public class HttpsProvisioningAlertDialog extends Activity {

    private AlertDialog mAlertDialog;
    private AutoDismissRunnable mDialogRunnable;
    private Handler mDialogHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.label_edit_msisdn);

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                HttpsProvionningMSISDNInput.getInstance().responseReceived(value);
                finish();
            }
        });

        builder.setNegativeButton(R.string.label_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                HttpsProvionningMSISDNInput.getInstance().responseReceived(null);
                finish();
            }
        });

        mAlertDialog = builder.show();
        mAlertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    mAlertDialog.dismiss();
                    finish();
                    return true;
                }
                return false;
            }
        });
        mDialogRunnable = new AutoDismissRunnable(this);

        mDialogHandler = new Handler();

        mDialogHandler.postDelayed(mDialogRunnable, HttpsProvisioningUtils.INPUT_MSISDN_TIMEOUT);
    }

    @Override
    protected void onDestroy() {

        mDialogHandler.removeCallbacks(mDialogRunnable);

        mAlertDialog.dismiss();

        super.onDestroy();
    }

    private class AutoDismissRunnable implements Runnable {

        WeakReference<HttpsProvisioningAlertDialog> activityWeak;

        public AutoDismissRunnable(HttpsProvisioningAlertDialog activity) {
            activityWeak = new WeakReference<HttpsProvisioningAlertDialog>(activity);
        }

        public void run() {
            HttpsProvionningMSISDNInput.getInstance().responseReceived(null);

            HttpsProvisioningAlertDialog activity = activityWeak.get();

            if (activity != null) {
                activity.mAlertDialog.dismiss();
                activity.finish();
            }
        }
    }
}
