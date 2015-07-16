/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 ******************************************************************************/

package com.gsma.iariauth.sample;

import com.gsma.iariauth.validator.PackageProcessor;
import com.gsma.iariauth.validator.ProcessingResult;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String IARI_COMMON_PREFIX = "urn:urn-7:3gpp-application.ims.iari.rcs.ext.ss.";
    private static final String IARI_AUTH_DOC_NAME_PREFIX = "iari_authorization.";
    private static final String IARI_AUTH_DOC_NAME_EXT = ".xml";

    private Map<Integer, String> mExtensionIds = new HashMap<Integer, String>();

    private String fingerPrint;

    private final static Integer[] RADIO_BUTTON_IDS = new Integer[] {
            R.id.radioButton1, R.id.radioButton2, R.id.radioButton3, R.id.radioButton4,
            R.id.radioButton5
    };

    private static final String TAG = LogUtils.getTag(MainActivity.class.getSimpleName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fingerPrint = FingerprintUtil.getFingerprint(this);
        if (fingerPrint == null) {
            Log.e(TAG, "Cant not get signature from application package certificate");
        }

        Log.d(TAG, "Application fingerprint: " + fingerPrint);
        Log.w(TAG, "Package name '" + getPackageName() + "'");

        init();
    }

    public void onRadioButtonClicked(View view) {
        RadioButton button = (RadioButton) view;
        if (button.isChecked()) {
            clearResult();
            int index = getRadioButtonIndex(view.getId());
            if (index != -1) {
                String extensionId = mExtensionIds.get(index);
                String filename = getIariDocumentFilename(index);
                checkExtension(extensionId, filename);
            } else {
                Log.w(TAG, "Cannot find extension index!");
            }
        }

    }

    private int getRadioButtonIndex(int id) {
        int index = 0;
        for (int i : RADIO_BUTTON_IDS) {
            if (i == id) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private String getIariDocumentFilename(int index) {
        return new StringBuilder(IARI_AUTH_DOC_NAME_PREFIX).append(index)
                .append(IARI_AUTH_DOC_NAME_EXT).toString();
    }

    /**
     * Init the view with extensions embedded in application package
     */
    private void init() {
        hideRadioButtons();
        List<String> extensionIds = ExtensionUtils.getExtensions(this);
        int i = 0;
        for (String extensionId : extensionIds) {
            mExtensionIds.put(i, extensionId);
            RadioButton rb = (RadioButton) findViewById(RADIO_BUTTON_IDS[i]);
            rb.setVisibility(View.VISIBLE);
            rb.setText(getIariDocumentFilename(i));
            i++;
        }
    }

    /**
     * Check if an extension is authorized
     * 
     * @param extensionId the specific part of the IARI string
     * @param iariDocumentFilename the IARI document filename
     */
    private void checkExtension(String extensionId, String iariDocumentFilename) {
        InputStream inStream = null;
        try {
            inStream = getAssets().open(iariDocumentFilename);
            appendMessage(iariDocumentFilename.concat(" :"));
            appendMessage("");

            appendMessage("Ckecking from IARI Tool : ");
            appendMessage(checkSecurityFromIARITool(extensionId, iariDocumentFilename, inStream)
                    + "\n");
            appendMessage("");
        } catch (IOException ioe) {
            Log.d(TAG, iariDocumentFilename.concat(" not found in assets"));
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * @param extensionId the specific part of the IARI string
     * @param iariDocumentFilename the IARI document filename
     * @param iariDoc the IARI document
     * @param report
     */
    private String checkSecurityFromIARITool(String extensionId, String iariDocumentFilename,
            InputStream iariDoc) {
        PackageProcessor processor = new PackageProcessor(getPackageName(), fingerPrint);
        ProcessingResult result = processor.processIARIauthorization(iariDoc);
        if (result.getStatus() == ProcessingResult.STATUS_OK) {
            String iari = result.getAuthDocument().iari;
            String expectedIari = IARI_COMMON_PREFIX.concat(extensionId);
            if (iari.equals(expectedIari)) {
                return iariDocumentFilename + " is authorized";
            } else {
                return iariDocumentFilename + " is not authorized: extension ID '" + extensionId
                        + "' does not match";
            }
        } else {
            return String.format("%1$s:\n%2$s", new Object[] {
                    "Extension is not authorized", result.getError()
            });
        }
    }

    private void hideRadioButtons() {
        for (int id : RADIO_BUTTON_IDS) {
            findViewById(id).setVisibility(View.INVISIBLE);
        }
    }

    private void clearResult() {
        ((TextView) findViewById(R.id.textView1)).setText("");
    }

    private void appendMessage(String message) {
        ((TextView) findViewById(R.id.textView1)).append(message + "\n");
    }

    /**
     * Returns the UID for the installed application
     * 
     * @param packageManager
     * @param packageName
     * @return
     */
    protected String getUidForPackage(PackageManager packageManager, String packageName) {
        try {
            int packageUid = packageManager.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA).uid;
            return String.valueOf(packageUid);
        } catch (NameNotFoundException e) {
            Log.w(TAG,
                    new StringBuilder(
                            "Package name not found in currently installed applications : ")
                            .append(packageName).toString());
        }
        return null;
    }

}
