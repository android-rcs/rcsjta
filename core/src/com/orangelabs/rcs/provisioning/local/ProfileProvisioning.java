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

package com.orangelabs.rcs.provisioning.local;

import static com.orangelabs.rcs.provisioning.local.Provisioning.saveCheckBoxParameter;
import static com.orangelabs.rcs.provisioning.local.Provisioning.saveEditTextParameter;
import static com.orangelabs.rcs.provisioning.local.Provisioning.setCheckBoxParameter;
import static com.orangelabs.rcs.provisioning.local.Provisioning.setEditTextParameter;
import static com.orangelabs.rcs.provisioning.local.Provisioning.setSpinnerParameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.ConfigurationMode;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.orangelabs.rcs.provisioning.ProvisioningParser;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * End user profile parameters provisioning
 * 
 * @author jexa7410
 */
public class ProfileProvisioning extends Activity {
	/**
	 * IMS authentication for mobile access
	 */
	private static final String[] MOBILE_IMS_AUTHENT = { AuthenticationProcedure.GIBA.name(), AuthenticationProcedure.DIGEST.name() };

	/**
	 * IMS authentication for Wi-Fi access
	 */
	private static final String[] WIFI_IMS_AUTHENT = { AuthenticationProcedure.DIGEST.name() };

	private static Logger logger = Logger.getLogger(ProfileProvisioning.class.getSimpleName());

	private static final String PROVISIONING_EXTENSION = ".xml";
	private String mInputedUserPhoneNumber = null;
	private String mSelectedProvisioningFile = null;

	private boolean isInFront;
	/**
	 * Folder path for provisioning file
	 */
	private static final String PROVISIONING_FOLDER_PATH = Environment.getExternalStorageDirectory().getPath();

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

		updateProfileProvisioningUI(savedInstanceState);
		isInFront = true;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (isInFront == false) {
			isInFront = true;
			// Update UI (from DB)
			updateProfileProvisioningUI(null);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		isInFront = false;
	}
	
	/**
	 * Update Profile Provisioning UI
	 * 
	 * @param bundle
	 *            bundle to save parameters
	 */
	private void updateProfileProvisioningUI(Bundle bundle) {
		// Display parameters
		Spinner spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForMobile);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(ProfileProvisioning.this, android.R.layout.simple_spinner_item,
				MOBILE_IMS_AUTHENT);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		setSpinnerParameter(spinner, RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE, bundle, MOBILE_IMS_AUTHENT);

		spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForWifi);
		adapter = new ArrayAdapter<String>(ProfileProvisioning.this, android.R.layout.simple_spinner_item, WIFI_IMS_AUTHENT);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(0);

		setEditTextParameter(this, R.id.ImsUsername, RcsSettingsData.USERPROFILE_IMS_USERNAME, bundle);
		setEditTextParameter(this, R.id.ImsDisplayName, RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, bundle);
		setEditTextParameter(this, R.id.ImsHomeDomain, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN, bundle);
		setEditTextParameter(this, R.id.ImsPrivateId, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, bundle);
		setEditTextParameter(this, R.id.ImsPassword, RcsSettingsData.USERPROFILE_IMS_PASSWORD, bundle);
		setEditTextParameter(this, R.id.ImsRealm, RcsSettingsData.USERPROFILE_IMS_REALM, bundle);
		setEditTextParameter(this, R.id.ImsOutboundProxyAddrForMobile, RcsSettingsData.IMS_PROXY_ADDR_MOBILE, bundle);
		setEditTextParameter(this, R.id.ImsOutboundProxyPortForMobile, RcsSettingsData.IMS_PROXY_PORT_MOBILE, bundle);
		setEditTextParameter(this, R.id.ImsOutboundProxyAddrForWifi, RcsSettingsData.IMS_PROXY_ADDR_WIFI, bundle);
		setEditTextParameter(this, R.id.ImsOutboundProxyPortForWifi, RcsSettingsData.IMS_PROXY_PORT_WIFI, bundle);
		setEditTextParameter(this, R.id.XdmServerAddr, RcsSettingsData.XDM_SERVER, bundle);
		setEditTextParameter(this, R.id.XdmServerLogin, RcsSettingsData.XDM_LOGIN, bundle);
		setEditTextParameter(this, R.id.XdmServerPassword, RcsSettingsData.XDM_PASSWORD, bundle);
		setEditTextParameter(this, R.id.FtHttpServerAddr, RcsSettingsData.FT_HTTP_SERVER, bundle);
		setEditTextParameter(this, R.id.FtHttpServerLogin, RcsSettingsData.FT_HTTP_LOGIN, bundle);
		setEditTextParameter(this, R.id.FtHttpServerPassword, RcsSettingsData.FT_HTTP_PASSWORD, bundle);
		setEditTextParameter(this, R.id.ImConferenceUri, RcsSettingsData.IM_CONF_URI, bundle);
		setEditTextParameter(this, R.id.EndUserConfReqUri, RcsSettingsData.ENDUSER_CONFIRMATION_URI, bundle);
		setEditTextParameter(this, R.id.RcsApn, RcsSettingsData.RCS_APN, bundle);

		setCheckBoxParameter(this, R.id.image_sharing, RcsSettingsData.CAPABILITY_IMAGE_SHARING, bundle);
		setCheckBoxParameter(this, R.id.video_sharing, RcsSettingsData.CAPABILITY_VIDEO_SHARING, bundle);
		setCheckBoxParameter(this, R.id.file_transfer, RcsSettingsData.CAPABILITY_FILE_TRANSFER, bundle);
		setCheckBoxParameter(this, R.id.file_transfer_http, RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP, bundle);
		setCheckBoxParameter(this, R.id.im, RcsSettingsData.CAPABILITY_IM_SESSION, bundle);
		setCheckBoxParameter(this, R.id.im_group, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION, bundle);
		setCheckBoxParameter(this, R.id.ipvoicecall, RcsSettingsData.CAPABILITY_IP_VOICE_CALL, bundle);
		setCheckBoxParameter(this, R.id.ipvideocall, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL, bundle);
		setCheckBoxParameter(this, R.id.cs_video, RcsSettingsData.CAPABILITY_CS_VIDEO, bundle);
		setCheckBoxParameter(this, R.id.presence_discovery, RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY, bundle);
		setCheckBoxParameter(this, R.id.social_presence, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, bundle);
		setCheckBoxParameter(this, R.id.geolocation_push, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, bundle);
		setCheckBoxParameter(this, R.id.file_transfer_thumbnail, RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL, bundle);
		setCheckBoxParameter(this, R.id.file_transfer_sf, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF, bundle);
		setCheckBoxParameter(this, R.id.group_chat_sf, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF, bundle);
		setCheckBoxParameter(this, R.id.sip_automata, RcsSettingsData.CAPABILITY_SIP_AUTOMATA, bundle);
		TextView txt = (TextView) findViewById(R.id.release);
		txt.setText(RcsSettings.getInstance().getGsmaRelease().name());
	}

	/**
	 * Save button listener
	 */
	private OnClickListener saveBtnListener = new OnClickListener() {
		public void onClick(View v) {
			// Save parameters
			saveInstanceState(null);
			Toast.makeText(ProfileProvisioning.this, getString(R.string.label_reboot_service), Toast.LENGTH_LONG).show();
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
		Spinner spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForMobile);
		if (bundle != null) {
			bundle.putInt(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE, spinner.getSelectedItemPosition());
		} else {
			AuthenticationProcedure procedure = AuthenticationProcedure.valueOf((String)spinner.getSelectedItem());
			RcsSettings.getInstance().setImsAuthenticationProcedureForMobile(procedure);
		}

		spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForWifi);
		if (bundle != null) {
			bundle.putInt(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI, spinner.getSelectedItemPosition());
		} else {
			AuthenticationProcedure procedure = AuthenticationProcedure.valueOf((String)spinner.getSelectedItem());
			RcsSettings.getInstance().setImsAuhtenticationProcedureForWifi(procedure);
		}

		saveEditTextParameter(this, R.id.ImsUsername, RcsSettingsData.USERPROFILE_IMS_USERNAME, bundle);
		saveEditTextParameter(this, R.id.ImsDisplayName, RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, bundle);
		saveEditTextParameter(this, R.id.ImsHomeDomain, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN, bundle);
		saveEditTextParameter(this, R.id.ImsPrivateId, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, bundle);
		saveEditTextParameter(this, R.id.ImsPassword, RcsSettingsData.USERPROFILE_IMS_PASSWORD, bundle);
		saveEditTextParameter(this, R.id.ImsRealm, RcsSettingsData.USERPROFILE_IMS_REALM, bundle);
		saveEditTextParameter(this, R.id.ImsOutboundProxyAddrForMobile, RcsSettingsData.IMS_PROXY_ADDR_MOBILE, bundle);
		saveEditTextParameter(this, R.id.ImsOutboundProxyPortForMobile, RcsSettingsData.IMS_PROXY_PORT_MOBILE, bundle);
		saveEditTextParameter(this, R.id.ImsOutboundProxyAddrForWifi, RcsSettingsData.IMS_PROXY_ADDR_WIFI, bundle);
		saveEditTextParameter(this, R.id.ImsOutboundProxyPortForWifi, RcsSettingsData.IMS_PROXY_PORT_WIFI, bundle);
		saveEditTextParameter(this, R.id.XdmServerAddr, RcsSettingsData.XDM_SERVER, bundle);
		saveEditTextParameter(this, R.id.XdmServerLogin, RcsSettingsData.XDM_LOGIN, bundle);
		saveEditTextParameter(this, R.id.XdmServerPassword, RcsSettingsData.XDM_PASSWORD, bundle);
		saveEditTextParameter(this, R.id.FtHttpServerAddr, RcsSettingsData.FT_HTTP_SERVER, bundle);
		saveEditTextParameter(this, R.id.FtHttpServerLogin, RcsSettingsData.FT_HTTP_LOGIN, bundle);
		saveEditTextParameter(this, R.id.FtHttpServerPassword, RcsSettingsData.FT_HTTP_PASSWORD, bundle);
		saveEditTextParameter(this, R.id.ImConferenceUri, RcsSettingsData.IM_CONF_URI, bundle);
		saveEditTextParameter(this, R.id.EndUserConfReqUri, RcsSettingsData.ENDUSER_CONFIRMATION_URI, bundle);
		saveEditTextParameter(this, R.id.RcsApn, RcsSettingsData.RCS_APN, bundle);

		// Save capabilities
		saveCheckBoxParameter(this, R.id.image_sharing, RcsSettingsData.CAPABILITY_IMAGE_SHARING, bundle);
		saveCheckBoxParameter(this, R.id.video_sharing, RcsSettingsData.CAPABILITY_VIDEO_SHARING, bundle);
		saveCheckBoxParameter(this, R.id.file_transfer, RcsSettingsData.CAPABILITY_FILE_TRANSFER, bundle);
		saveCheckBoxParameter(this, R.id.file_transfer_http, RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP, bundle);
		saveCheckBoxParameter(this, R.id.im, RcsSettingsData.CAPABILITY_IM_SESSION, bundle);
		saveCheckBoxParameter(this, R.id.im_group, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION, bundle);
		saveCheckBoxParameter(this, R.id.ipvoicecall, RcsSettingsData.CAPABILITY_IP_VOICE_CALL, bundle);
		saveCheckBoxParameter(this, R.id.ipvideocall, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL, bundle);
		saveCheckBoxParameter(this, R.id.cs_video, RcsSettingsData.CAPABILITY_CS_VIDEO, bundle);
		saveCheckBoxParameter(this, R.id.presence_discovery, RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY, bundle);
		saveCheckBoxParameter(this, R.id.social_presence, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, bundle);
		saveCheckBoxParameter(this, R.id.geolocation_push, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, bundle);
		saveCheckBoxParameter(this, R.id.file_transfer_thumbnail, RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL, bundle);
		saveCheckBoxParameter(this, R.id.file_transfer_sf, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF, bundle);
		saveCheckBoxParameter(this, R.id.group_chat_sf, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF, bundle);
		saveCheckBoxParameter(this, R.id.sip_automata, RcsSettingsData.CAPABILITY_SIP_AUTOMATA, bundle);
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

	/**
	 * Load the user profile
	 */
	private void loadProfile() {
		LayoutInflater factory = LayoutInflater.from(this);
		final View view = factory.inflate(R.layout.rcs_provisioning_generate_profile, null);
		final EditText textEdit = (EditText) view.findViewById(R.id.msisdn);
		textEdit.setText(RcsSettings.getInstance().getUserProfileImsUserName());

		String[] xmlFiles = getProvisioningFiles();
		final Spinner spinner = (Spinner) view.findViewById(R.id.XmlProvisioningFile);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, xmlFiles);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.label_generate_profile).setView(view)
				.setNegativeButton(R.string.label_cancel, null)
				.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						mInputedUserPhoneNumber = textEdit.getText().toString();
						mSelectedProvisioningFile = (String) spinner.getSelectedItem();
						if (mSelectedProvisioningFile != null
								&& !mSelectedProvisioningFile.equals(getString(R.string.label_no_xml_file))) {
							String filePath = PROVISIONING_FOLDER_PATH + File.separator + mSelectedProvisioningFile;
							if (logger.isActivated()) {
								logger.debug("Selection of provisioning file: " + mSelectedProvisioningFile);
							}
							String mXMLFileContent = getFileContent(filePath);
							if (mXMLFileContent != null) {
								if (logger.isActivated()) {
									logger.debug("Selection of provisioning file: " + filePath);
								}
								ProvisionTask mProvisionTask = new ProvisionTask();
								mProvisionTask.execute(mXMLFileContent, mInputedUserPhoneNumber);
								return;
							}
						}
						Toast.makeText(ProfileProvisioning.this, getString(R.string.label_load_failed), Toast.LENGTH_LONG).show();
					}
				});
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}

	/**
	 * Read a text file and convert it into a string
	 * 
	 * @param filePath
	 *            the file path
	 * @return the result string
	 */
	private String getFileContent(String filePath) {
		if (filePath == null)
			return null;
		// Get the text file
		File file = new File(filePath);

		// Read text from file
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

		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Error reading file content: " + e.getClass().getName() + " " + e.getMessage(), e);
			}
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
				}
		}
		return null;
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
			String UserPhoneNumber = params[1];
			String mXMLFileContent = params[0];
			return createProvisioning(mXMLFileContent, UserPhoneNumber);
		}

		/**
		 * Parse the provisioning data then save it into RCS settings provider
		 * 
		 * @param mXMLFileContent
		 *            the XML file containing provisioning data
		 * @param userPhoneNumber
		 *            the user phone number
		 * @return true if loading the provisioning is successful
		 */
		private boolean createProvisioning(String mXMLFileContent, String userPhoneNumber) {
			RcsSettings rcsSettings = RcsSettings.getInstance();
			ProvisioningParser parser = new ProvisioningParser(mXMLFileContent, rcsSettings);
			// Save GSMA release set into the provider
			GsmaRelease release = rcsSettings.getGsmaRelease();
			// Save client Messaging Mode set into the provider
			MessagingMode messagingMode = rcsSettings.getMessagingMode();
			
			// Before parsing the provisioning, the GSMA release is set to Albatros
			rcsSettings.setGsmaRelease(GsmaRelease.ALBATROS);
			// Before parsing the provisioning, the client Messaging mode is set to NONE 
			rcsSettings.setMessagingMode(MessagingMode.NONE);
			
			if (parser.parse(release,true)) {
				// Customize provisioning data with user phone number
				rcsSettings.writeParameter(RcsSettingsData.USERPROFILE_IMS_USERNAME, userPhoneNumber);
				rcsSettings.writeParameter(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, userPhoneNumber);
				String homeDomain = rcsSettings.readParameter(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN);
				String sipUri = userPhoneNumber + "@" + homeDomain;
				rcsSettings.writeParameter(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, sipUri);
				rcsSettings.writeParameter(RcsSettingsData.FT_HTTP_LOGIN, sipUri);
				return true;
			} else {
				if (logger.isActivated()) {
					logger.error("Can't parse provisioning document");
				}
				// Restore GSMA release saved before parsing of the provisioning
				rcsSettings.setGsmaRelease(release);
				// Restore the client messaging mode saved before parsing of the provisioning
				rcsSettings.setMessagingMode(messagingMode);
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			updateProfileProvisioningUI(null);
			// set configuration mode to manual
			RcsSettings.getInstance().setConfigurationMode(ConfigurationMode.MANUAL);
			if (result)
				Toast.makeText(ProfileProvisioning.this, getString(R.string.label_reboot_service), Toast.LENGTH_LONG).show();
			else
				Toast.makeText(ProfileProvisioning.this, getString(R.string.label_parse_failed), Toast.LENGTH_LONG).show();
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
			return new String[] { getString(R.string.label_no_xml_file) };
		} else {
			return files;
		}
	}
}
