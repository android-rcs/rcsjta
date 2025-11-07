/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provisioning.local;

import com.gsma.rcs.R;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.provisioning.ProvisioningParser;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.contact.ContactId;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * A tool to handle the provisioning locally
 *
 * @author Philippe LEMORDANT
 */
public class Provisioning extends AppCompatActivity {

    private static final String PROVISIONING_EXTENSION = ".xml";
    private static final int MY_PERMISSION_REQUEST_ALL = 5428;

    /**
     * The XML provisioning file loaded manually contains a MSISDN token which must be replaced by
     * the actual value
     */
    private static final String TOKEN_MSISDN = "__s__MSISDN__e__";

    /**
     * Folder path for provisioning file
     */
    private static final String PROVISIONING_FOLDER_PATH = Environment
            .getExternalStorageDirectory().getPath();
    private String[] titles = new String[] {
            "Profile", "Stack", "Service", "Capabilities", "Logger"
    };

    private static final Logger sLogger = Logger.getLogger(Provisioning.class.getName());
    private ViewPagerAdapter mAdapter;
    private RcsSettings mRcsSettings;
    private Provisioning mActivity;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.provisioning);

        LocalContentResolver localContentResolver = new LocalContentResolver(
                getApplicationContext());
        mRcsSettings = RcsSettings.getInstance(localContentResolver);
        AndroidFactory.setApplicationContext(this, mRcsSettings);

        ViewPager pager = (ViewPager) findViewById(R.id.viewpager);
        SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mAdapter = new ViewPagerAdapter(getSupportFragmentManager(), titles, mRcsSettings);
        pager.setAdapter(mAdapter);

        slidingTabLayout.setViewPager(pager);
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return Color.WHITE;
            }
        });

        IntentFilter filter = new IntentFilter(RcsService.ACTION_SERVICE_UP);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for (IProvisioningFragment fragment : mAdapter.getFragments()) {
                    fragment.displayRcsSettings();
                }
            }
        };
        registerReceiver(mReceiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_provisioning, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.save) {
            if (sLogger.isActivated()) {
                sLogger.debug("Save provisioning");
            }
            for (IProvisioningFragment fragment : mAdapter.getFragments()) {
                fragment.persistRcsSettings();
            }
            Toast.makeText(this, getString(R.string.label_reboot_service), Toast.LENGTH_LONG)
                    .show();
            return true;
        } else if (itemId == R.id.load) {
            if (sLogger.isActivated()) {
                sLogger.debug("Load provisioning");
            }
            loadXmlFile();
            return true;
        } else if (itemId == R.id.about) {
            displayInfo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Returns the application version from manifest file
     *
     * @return Application version or null if not found
     */
    private String getAppVersion() {
        String version = "";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    RcsServiceControl.RCS_STACK_PACKAGENAME, 0);
            version = info.versionName + "." + info.versionCode;

        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return version;
    }

    /**
     * Returns the GSMA version
     *
     * @return String
     */
    private String getGsmaVersion() {
        StringBuilder version = new StringBuilder("API ");
        version.append(RcsService.Build.API_CODENAME);
        version.append(" ");
        switch (RcsService.Build.API_VERSION) {
            case RcsService.Build.VERSION_CODES.BASE:
                version.append("Albatros 2.0.");
                break;
            case RcsService.Build.VERSION_CODES.BLACKBIRD:
                version.append("Blackbird 1.5.");
                break;
            case RcsService.Build.VERSION_CODES.CPR:
                version.append("Crane PR 1.6.");
                break;
            default:
                version.append("Unknown 0.0");
        }
        version.append(RcsService.Build.API_INCREMENTAL);
        return version.toString();
    }

    private void displayInfo() {
        View view = View.inflate(mActivity, R.layout.rcs_provisioning_about, null);
        // Display application release
        TextView releaseView = (TextView) view.findViewById(R.id.app_version);
        releaseView.setText(getString(R.string.label_about_app_version, getAppVersion()));
        // Display GSMA version
        TextView gsmaView = (TextView) view.findViewById(R.id.gsma_version);
        gsmaView.setText(getGsmaVersion());
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.label_about)
                .setView(view).setPositiveButton(R.string.label_ok, null);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (MY_PERMISSION_REQUEST_ALL == requestCode) {
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                // SDCARD permission has been granted, preview can be displayed
                if (sLogger.isActivated()) {
                    sLogger.debug("SDCARD permission has now been granted");
                }
                loadXmlFile();

            } else {
                if (sLogger.isActivated()) {
                    sLogger.info("SDCARD read permission was not granted!");
                }
                Toast.makeText(mActivity, getString(R.string.label_sdcard_permission_not_granted),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadXmlFile() {
        final boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("load XML provisioning File");
        }
        try {
            String[] xmlFiles = getProvisioningFiles();
            View view = View.inflate(mActivity, R.layout.rcs_provisioning_generate_profile, null);
            final EditText textEdit = (EditText) view.findViewById(R.id.msisdn);
            ContactId me = mRcsSettings.getUserProfileImsUserName();
            textEdit.setText(me == null ? "" : me.toString());
            final Spinner spinner = (Spinner) view.findViewById(R.id.XmlProvisioningFile);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, xmlFiles);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.label_generate_profile).setView(view)
                    .setNegativeButton(R.string.label_cancel, null)
                    .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ContactUtil.PhoneNumber number = ContactUtil
                                    .getValidPhoneNumberFromAndroid(textEdit.getText().toString());
                            if (number == null) {
                                Toast.makeText(mActivity, getString(R.string.label_load_failed),
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            ContactId contact = ContactUtil
                                    .createContactIdFromValidatedData(number);
                            String selectedProvisioningFile = (String) spinner.getSelectedItem();
                            if (selectedProvisioningFile == null
                                    || selectedProvisioningFile
                                            .equals(getString(R.string.label_no_xml_file))) {
                                Toast.makeText(mActivity, getString(R.string.label_load_failed),
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            loadProfile(contact, Uri.fromFile(new File(PROVISIONING_FOLDER_PATH,
                                    selectedProvisioningFile)));
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

        } catch (SecurityException e) {
            if (logActivated) {
                sLogger.warn("Failed to load provisioning file!", e);
            }
        }
    }

    private String[] getProvisioningFiles() {
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, MY_PERMISSION_REQUEST_ALL);
            throw new SecurityException("Permission not granted to access SD card!");
        }
        String[] files = null;
        File folder = new File(PROVISIONING_FOLDER_PATH);
        // noinspection ResultOfMethodCallIgnored
        folder.mkdirs();
        if (folder.exists()) {
            // filter
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(PROVISIONING_EXTENSION);
                }
            };
            files = folder.list(filter);
        }
        if (files == null || files.length == 0) {
            // No provisioning file
            return new String[] {
                getString(R.string.label_no_xml_file)
            };
        }
        return files;
    }

    private void loadProfile(ContactId contact, Uri provisioningFile) {
        final boolean logActivated = sLogger.isActivated();
        try {
            if (logActivated) {
                sLogger.debug("Selection of provisioning file: ".concat(provisioningFile.getPath()));
            }
            String xMLFileContent = getFileContent(provisioningFile);
            LoadXmlPovisioningTask mProvisionTask = new LoadXmlPovisioningTask();
            mProvisionTask.execute(xMLFileContent, contact.toString());

        } catch (IOException e) {
            if (logActivated) {
                sLogger.debug("Loading of provisioning failed: invalid XML file '"
                        + provisioningFile + "', Message=" + e.getMessage());
            }
            Toast.makeText(mActivity, getString(R.string.label_load_failed), Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Read a text file and convert it into a string
     *
     * @param provisioningFile Uri for the file
     * @return the result string
     * @throws IOException
     */
    private String getFileContent(Uri provisioningFile) throws IOException {
        File file = new File(provisioningFile.getPath());
        StringBuilder text = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            return text.toString();

        } finally {
            // noinspection ThrowableResultOfMethodCallIgnored
            CloseableUtils.tryToClose(br);
        }
    }

    /**
     * Asynchronous Tasks that loads the provisioning file.
     */
    private class LoadXmlPovisioningTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            ContactId UserPhoneNumber = ContactUtil.createContactIdFromTrustedData(params[1]);
            String mXMLFileContent = params[0];
            return createProvisioning(mXMLFileContent, UserPhoneNumber);
        }

        /**
         * Parse the provisioning data then save it into RCS settings provider
         *
         * @param xmlFileContent the XML file containing provisioning data
         * @param myContact the user phone number
         * @return true if loading the provisioning is successful
         */
        private boolean createProvisioning(String xmlFileContent, ContactId myContact) {
            String phoneNumber = myContact.toString();
            String configToParse = xmlFileContent
                    .replaceAll(TOKEN_MSISDN, phoneNumber.substring(1));
            ProvisioningParser parser = new ProvisioningParser(configToParse, mRcsSettings);
            // Save GSMA release set into the provider
            RcsSettingsData.GsmaRelease release = mRcsSettings.getGsmaRelease();
            // Save client Messaging Mode set into the provider
            CommonServiceConfiguration.MessagingMode messagingMode = mRcsSettings
                    .getMessagingMode();
            // Before parsing the provisioning, the GSMA release is set to Albatros
            mRcsSettings.setGsmaRelease(RcsSettingsData.GsmaRelease.ALBATROS);
            // Before parsing the provisioning, the client Messaging mode is set to NONE
            mRcsSettings.setMessagingMode(CommonServiceConfiguration.MessagingMode.NONE);
            try {
                parser.parse(release, messagingMode, true);
                /* Customize display name with user phone number */
                mRcsSettings.setUserProfileImsDisplayName(phoneNumber);
                mRcsSettings.setFileTransferHttpSupported(mRcsSettings.getFtHttpServer() != null
                        && mRcsSettings.getFtHttpLogin() != null
                        && mRcsSettings.getFtHttpPassword() != null);
                return true;

            } catch (SAXException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug(e.getMessage());
                }
                // Restore GSMA release saved before parsing of the provisioning
                mRcsSettings.setGsmaRelease(release);
                // Restore the client messaging mode saved before parsing of the provisioning
                mRcsSettings.setMessagingMode(messagingMode);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            // set configuration mode to manual
            mRcsSettings.setConfigurationMode(RcsSettingsData.ConfigurationMode.MANUAL);
            for (IProvisioningFragment fragment : mAdapter.getFragments()) {
                fragment.displayRcsSettings();
            }
            if (result)
                Toast.makeText(mActivity, getString(R.string.label_reboot_service),
                        Toast.LENGTH_LONG).show();
            else
                Toast.makeText(mActivity, getString(R.string.label_parse_failed), Toast.LENGTH_LONG)
                        .show();
        }
    }
}
