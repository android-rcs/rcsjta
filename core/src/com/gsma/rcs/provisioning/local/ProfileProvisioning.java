/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provisioning.local;

import static com.gsma.rcs.provisioning.local.Provisioning.saveCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveContactIdEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveIntegerEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveStringEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveUriEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setContactIdEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setIntegerEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setSpinnerParameter;
import static com.gsma.rcs.provisioning.local.Provisioning.setStringEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setUriEditTextParam;

import com.gsma.rcs.R;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.gsma.rcs.provider.settings.RcsSettingsData.ConfigurationMode;
import com.gsma.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.gsma.rcs.provisioning.ProvisioningParser;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.gsma.services.rcs.contact.ContactId;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
 * End user profile parameters provisioning
 * 
 * @author jexa7410
 * @author Philippe LEMORDANT
 */
public class ProfileProvisioning extends Activity {

    /**
     * The XML provisioning file loaded manually contains a MSISDN token which must be replaced by
     * the actual value
     */
    private static final String TOKEN_MSISDN = "__s__MSISDN__e__";

    /**
     * IMS authentication for mobile access
     */
    private static final String[] MOBILE_IMS_AUTHENT = {
            AuthenticationProcedure.GIBA.name(), AuthenticationProcedure.DIGEST.name()
    };

    /**
     * IMS authentication for Wi-Fi access
     */
    private static final String[] WIFI_IMS_AUTHENT = {
        AuthenticationProcedure.DIGEST.name()
    };

    private static final Logger sLogger = Logger.getLogger(ProfileProvisioning.class.getName());

    private static final String PROVISIONING_EXTENSION = ".xml";

    private boolean mInFront;

    private RcsSettings mRcsSettings;

    /**
     * Folder path for provisioning file
     */
    private static final String PROVISIONING_FOLDER_PATH = Environment
            .getExternalStorageDirectory().getPath();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.rcs_provisioning_profile);

        // Set buttons callback
        Button btn = (Button) findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        btn = (Button) findViewById(R.id.gen_btn);
        btn.setOnClickListener(genBtnListener);

        mRcsSettings = RcsSettings.createInstance(new LocalContentResolver(this));

        updateProfileProvisioningUI(savedInstanceState);
        mInFront = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mInFront) {
            mInFront = true;
            // Update UI (from DB)
            updateProfileProvisioningUI(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mInFront = false;
    }

    /**
     * Update Profile Provisioning UI
     * 
     * @param bundle bundle to save parameters
     */
    private void updateProfileProvisioningUI(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        // Display parameters
        Spinner spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForMobile);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ProfileProvisioning.this,
                android.R.layout.simple_spinner_item, MOBILE_IMS_AUTHENT);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        setSpinnerParameter(spinner, RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE, false,
                MOBILE_IMS_AUTHENT, helper);

        spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForWifi);
        adapter = new ArrayAdapter<>(ProfileProvisioning.this,
                android.R.layout.simple_spinner_item, WIFI_IMS_AUTHENT);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);

        setContactIdEditTextParam(R.id.ImsUsername, RcsSettingsData.USERPROFILE_IMS_USERNAME,
                helper);
        setStringEditTextParam(R.id.ImsDisplayName, RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME,
                helper);
        setStringEditTextParam(R.id.ImsHomeDomain, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN,
                helper);
        setStringEditTextParam(R.id.ImsPrivateId, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID,
                helper);
        setStringEditTextParam(R.id.ImsPassword, RcsSettingsData.USERPROFILE_IMS_PASSWORD, helper);
        setStringEditTextParam(R.id.ImsRealm, RcsSettingsData.USERPROFILE_IMS_REALM, helper);
        setStringEditTextParam(R.id.ImsOutboundProxyAddrForMobile,
                RcsSettingsData.IMS_PROXY_ADDR_MOBILE, helper);
        setIntegerEditTextParam(R.id.ImsOutboundProxyPortForMobile,
                RcsSettingsData.IMS_PROXY_PORT_MOBILE, helper);
        setStringEditTextParam(R.id.ImsOutboundProxyAddrForWifi,
                RcsSettingsData.IMS_PROXY_ADDR_WIFI, helper);
        setIntegerEditTextParam(R.id.ImsOutboundProxyPortForWifi,
                RcsSettingsData.IMS_PROXY_PORT_WIFI, helper);
        setUriEditTextParam(R.id.XdmServerAddr, RcsSettingsData.XDM_SERVER, helper);
        setStringEditTextParam(R.id.XdmServerLogin, RcsSettingsData.XDM_LOGIN, helper);
        setStringEditTextParam(R.id.XdmServerPassword, RcsSettingsData.XDM_PASSWORD, helper);
        setUriEditTextParam(R.id.FtHttpServerAddr, RcsSettingsData.FT_HTTP_SERVER, helper);
        setStringEditTextParam(R.id.FtHttpServerLogin, RcsSettingsData.FT_HTTP_LOGIN, helper);
        setStringEditTextParam(R.id.FtHttpServerPassword, RcsSettingsData.FT_HTTP_PASSWORD, helper);
        setUriEditTextParam(R.id.ImConferenceUri, RcsSettingsData.IM_CONF_URI, helper);
        setUriEditTextParam(R.id.EndUserConfReqUri, RcsSettingsData.ENDUSER_CONFIRMATION_URI,
                helper);
        setStringEditTextParam(R.id.RcsApn, RcsSettingsData.RCS_APN, helper);

        setCheckBoxParam(R.id.image_sharing, RcsSettingsData.CAPABILITY_IMAGE_SHARING, helper);
        setCheckBoxParam(R.id.video_sharing, RcsSettingsData.CAPABILITY_VIDEO_SHARING, helper);
        setCheckBoxParam(R.id.file_transfer_msrp, RcsSettingsData.CAPABILITY_FILE_TRANSFER, helper);
        setCheckBoxParam(R.id.file_transfer_http, RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP,
                helper);
        setCheckBoxParam(R.id.im, RcsSettingsData.CAPABILITY_IM_SESSION, helper);
        setCheckBoxParam(R.id.im_group, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION, helper);
        setCheckBoxParam(R.id.ipvoicecall, RcsSettingsData.CAPABILITY_IP_VOICE_CALL, helper);
        setCheckBoxParam(R.id.ipvideocall, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL, helper);
        setCheckBoxParam(R.id.cs_video, RcsSettingsData.CAPABILITY_CS_VIDEO, helper);
        setCheckBoxParam(R.id.presence_discovery, RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                helper);
        setCheckBoxParam(R.id.social_presence, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, helper);
        setCheckBoxParam(R.id.geolocation_push, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, helper);
        setCheckBoxParam(R.id.file_transfer_thumbnail,
                RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL, helper);
        setCheckBoxParam(R.id.file_transfer_sf, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF, helper);
        setCheckBoxParam(R.id.group_chat_sf, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF, helper);
        setCheckBoxParam(R.id.sip_automata, RcsSettingsData.CAPABILITY_SIP_AUTOMATA, helper);
        TextView txt = (TextView) findViewById(R.id.release);
        txt.setText(mRcsSettings.getGsmaRelease().name());
        txt = (TextView) findViewById(R.id.user_msg_title);
        String title = mRcsSettings.getProvisioningUserMessageTitle();
        if (title != null) {
            txt.setText(title);
        }
        txt = (TextView) findViewById(R.id.user_msg_content);
        String content = mRcsSettings.getProvisioningUserMessageContent();
        if (content != null) {
            txt.setText(content);
        }
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
            // Save parameters
            saveInstanceState(null);
            Toast.makeText(ProfileProvisioning.this, getString(R.string.label_reboot_service),
                    Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        saveInstanceState(bundle);
    }

    /**
     * Save parameters either in bundle or in RCS settings
     */
    private void saveInstanceState(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        Spinner spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForMobile);
        if (bundle != null) {
            bundle.putInt(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE,
                    spinner.getSelectedItemPosition());
        } else {
            AuthenticationProcedure procedure = AuthenticationProcedure.valueOf((String) spinner
                    .getSelectedItem());
            mRcsSettings.setImsAuthenticationProcedureForMobile(procedure);
        }

        spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForWifi);
        if (bundle != null) {
            bundle.putInt(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI,
                    spinner.getSelectedItemPosition());
        } else {
            AuthenticationProcedure procedure = AuthenticationProcedure.valueOf((String) spinner
                    .getSelectedItem());
            mRcsSettings.setImsAuhtenticationProcedureForWifi(procedure);
        }

        saveContactIdEditTextParam(R.id.ImsUsername, RcsSettingsData.USERPROFILE_IMS_USERNAME,
                helper);
        saveStringEditTextParam(R.id.ImsDisplayName, RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME,
                helper);
        saveStringEditTextParam(R.id.ImsHomeDomain, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN,
                helper);
        saveStringEditTextParam(R.id.ImsPrivateId, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID,
                helper);
        saveStringEditTextParam(R.id.ImsPassword, RcsSettingsData.USERPROFILE_IMS_PASSWORD, helper);
        saveStringEditTextParam(R.id.ImsRealm, RcsSettingsData.USERPROFILE_IMS_REALM, helper);
        saveStringEditTextParam(R.id.ImsOutboundProxyAddrForMobile,
                RcsSettingsData.IMS_PROXY_ADDR_MOBILE, helper);
        saveIntegerEditTextParam(R.id.ImsOutboundProxyPortForMobile,
                RcsSettingsData.IMS_PROXY_PORT_MOBILE, helper);
        saveStringEditTextParam(R.id.ImsOutboundProxyAddrForWifi,
                RcsSettingsData.IMS_PROXY_ADDR_WIFI, helper);
        saveIntegerEditTextParam(R.id.ImsOutboundProxyPortForWifi,
                RcsSettingsData.IMS_PROXY_PORT_WIFI, helper);
        saveUriEditTextParam(R.id.XdmServerAddr, RcsSettingsData.XDM_SERVER, helper);
        saveStringEditTextParam(R.id.XdmServerLogin, RcsSettingsData.XDM_LOGIN, helper);
        saveStringEditTextParam(R.id.XdmServerPassword, RcsSettingsData.XDM_PASSWORD, helper);
        saveUriEditTextParam(R.id.FtHttpServerAddr, RcsSettingsData.FT_HTTP_SERVER, helper);
        saveStringEditTextParam(R.id.FtHttpServerLogin, RcsSettingsData.FT_HTTP_LOGIN, helper);
        saveStringEditTextParam(R.id.FtHttpServerPassword, RcsSettingsData.FT_HTTP_PASSWORD, helper);

        if (bundle == null) {
            mRcsSettings.setFileTransferHttpSupported(mRcsSettings.getFtHttpServer() != null
                    && mRcsSettings.getFtHttpLogin() != null
                    && mRcsSettings.getFtHttpPassword() != null);
        }

        saveUriEditTextParam(R.id.ImConferenceUri, RcsSettingsData.IM_CONF_URI, helper);
        saveUriEditTextParam(R.id.EndUserConfReqUri, RcsSettingsData.ENDUSER_CONFIRMATION_URI,
                helper);
        saveStringEditTextParam(R.id.RcsApn, RcsSettingsData.RCS_APN, helper);

        // Save capabilities
        saveCheckBoxParam(R.id.image_sharing, RcsSettingsData.CAPABILITY_IMAGE_SHARING, helper);
        saveCheckBoxParam(R.id.video_sharing, RcsSettingsData.CAPABILITY_VIDEO_SHARING, helper);
        saveCheckBoxParam(R.id.file_transfer_msrp, RcsSettingsData.CAPABILITY_FILE_TRANSFER, helper);
        saveCheckBoxParam(R.id.file_transfer_http, RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP,
                helper);
        saveCheckBoxParam(R.id.im, RcsSettingsData.CAPABILITY_IM_SESSION, helper);
        saveCheckBoxParam(R.id.im_group, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION, helper);
        saveCheckBoxParam(R.id.ipvoicecall, RcsSettingsData.CAPABILITY_IP_VOICE_CALL, helper);
        saveCheckBoxParam(R.id.ipvideocall, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL, helper);
        saveCheckBoxParam(R.id.cs_video, RcsSettingsData.CAPABILITY_CS_VIDEO, helper);
        saveCheckBoxParam(R.id.presence_discovery, RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                helper);
        saveCheckBoxParam(R.id.social_presence, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, helper);
        saveCheckBoxParam(R.id.geolocation_push, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH,
                helper);
        saveCheckBoxParam(R.id.file_transfer_thumbnail,
                RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL, helper);
        saveCheckBoxParam(R.id.file_transfer_sf, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF,
                helper);
        saveCheckBoxParam(R.id.group_chat_sf, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF, helper);
        saveCheckBoxParam(R.id.sip_automata, RcsSettingsData.CAPABILITY_SIP_AUTOMATA, helper);
    }

    /**
     * Generate profile button listener
     */
    private OnClickListener genBtnListener = new OnClickListener() {
        public void onClick(View v) {
            // Load the user profile
            loadProfile();
        }
    };

    private void loadProfile(ContactId contact, Uri provisioningFile) {
        final boolean logActivated = sLogger.isActivated();
        try {
            if (logActivated) {
                sLogger.debug("Selection of provisioning file: ".concat(provisioningFile.getPath()));
            }
            String xMLFileContent = getFileContent(provisioningFile);
            ProvisionTask mProvisionTask = new ProvisionTask();
            mProvisionTask.execute(xMLFileContent, contact.toString());
        } catch (IOException e) {
            if (logActivated) {
                sLogger.debug("Loading of provisioning failed: invalid XML file '" +
                        provisioningFile + "', Message=" + e.getMessage());
            }
            Toast.makeText(ProfileProvisioning.this, getString(R.string.label_load_failed),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load the user profile
     */
    private void loadProfile() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.rcs_provisioning_generate_profile, null);
        final EditText textEdit = (EditText) view.findViewById(R.id.msisdn);
        ContactId me = mRcsSettings.getUserProfileImsUserName();
        textEdit.setText(me == null ? "" : me.toString());

        String[] xmlFiles = getProvisioningFiles();
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
                        PhoneNumber number = ContactUtil.getValidPhoneNumberFromAndroid(textEdit
                                .getText().toString());
                        if (number == null) {
                            Toast.makeText(ProfileProvisioning.this,
                                    getString(R.string.label_load_failed), Toast.LENGTH_LONG)
                                    .show();
                            return;
                        }
                        ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
                        String selectedProvisioningFile = (String) spinner.getSelectedItem();
                        if (selectedProvisioningFile == null
                                || selectedProvisioningFile
                                        .equals(getString(R.string.label_no_xml_file))) {
                            Toast.makeText(ProfileProvisioning.this,
                                    getString(R.string.label_load_failed), Toast.LENGTH_LONG)
                                    .show();
                            return;
                        }
                        loadProfile(contact, Uri.fromFile(new File(PROVISIONING_FOLDER_PATH,
                                selectedProvisioningFile)));
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
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
            //noinspection ThrowableResultOfMethodCallIgnored
            CloseableUtils.tryToClose(br);
        }
    }

    /**
     * Asynchronous Tasks that loads the provisioning file.
     */
    private class ProvisionTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

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
            GsmaRelease release = mRcsSettings.getGsmaRelease();
            // Save client Messaging Mode set into the provider
            MessagingMode messagingMode = mRcsSettings.getMessagingMode();

            // Before parsing the provisioning, the GSMA release is set to Albatros
            mRcsSettings.setGsmaRelease(GsmaRelease.ALBATROS);
            // Before parsing the provisioning, the client Messaging mode is set to NONE
            mRcsSettings.setMessagingMode(MessagingMode.NONE);
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
            updateProfileProvisioningUI(null);
            // set configuration mode to manual
            mRcsSettings.setConfigurationMode(ConfigurationMode.MANUAL);
            if (result)
                Toast.makeText(ProfileProvisioning.this, getString(R.string.label_reboot_service),
                        Toast.LENGTH_LONG).show();
            else
                Toast.makeText(ProfileProvisioning.this, getString(R.string.label_parse_failed),
                        Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load a list of provisioning files from the SDCARD
     * 
     * @return List of XML provisioning files
     */
    private String[] getProvisioningFiles() {
        String[] files = null;
        File folder = new File(PROVISIONING_FOLDER_PATH);
        try {
            //noinspection ResultOfMethodCallIgnored
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
        } catch (SecurityException e) {
            // intentionally blank
        }
        if ((files == null) || (files.length == 0)) {
            // No provisioning file
            return new String[] {
                getString(R.string.label_no_xml_file)
            };
        }
        return files;
    }
}
