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

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;

/**
 * Stack parameters provisioning
 *File
 * @author jexa7410
 */
public class StackProvisioning extends Activity {
    /**
     * Folder path for certificate
     */
    public static final String CERTIFICATE_FOLDER_PATH = Environment.getExternalStorageDirectory().getPath();

	/**
	 * Auto config mode
	 */
    private static final String[] AUTO_CONFIG = {
        "None", "HTTPS"
    };    
    
    /**
	 * SIP protocol
	 */
    private static final String[] SIP_PROTOCOL = {
        "UDP", "TCP", "TLS"
    };

	/**
	 * Network access
	 */
    private static final String[] NETWORK_ACCESS = {
    	"All networks", "Mobile only", "Wi-Fi only"
    };

    /**
     * FT protocol
     */
    private static final String[] FT_PROTOCOL = {
        RcsSettingsData.FT_PROTOCOL_HTTP, RcsSettingsData.FT_PROTOCOL_MSRP
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.rcs_provisioning_stack);
        
		// Set buttons callback
        Button btn = (Button)findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);        
    }

    @Override
    protected void onResume() {
    	super.onResume();

        // Display stack parameters
        Spinner spinner = (Spinner)findViewById(R.id.Autoconfig);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, AUTO_CONFIG);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int mode = RcsSettings.getInstance().getAutoConfigMode();
        if (mode == RcsSettingsData.HTTPS_AUTO_CONFIG) {
            spinner.setSelection(1);
        } else {
            spinner.setSelection(0);
        }

        EditText txt = (EditText)this.findViewById(R.id.SecondaryProvisioningAddress);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS));

        CheckBox check = (CheckBox)this.findViewById(R.id.SecondaryProvisioningAddressOnly);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY)));

        spinner = (Spinner)findViewById(R.id.NetworkAccess);
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, NETWORK_ACCESS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int access = RcsSettings.getInstance().getNetworkAccess();
        if (access == RcsSettingsData.WIFI_ACCESS) {
            spinner.setSelection(2);
        } else
        if (access == RcsSettingsData.MOBILE_ACCESS) {
            spinner.setSelection(1);
        } else {
            spinner.setSelection(0);
        }

        spinner = (Spinner) findViewById(R.id.SipDefaultProtocolForMobile);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, SIP_PROTOCOL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String sipMobile = RcsSettings.getInstance().getSipDefaultProtocolForMobile();
        if (sipMobile.equalsIgnoreCase(SIP_PROTOCOL[0])) {
            spinner.setSelection(0);
        } else
        if (sipMobile.equalsIgnoreCase(SIP_PROTOCOL[1])) {
            spinner.setSelection(1);
        } else {
            spinner.setSelection(2);
        }

        spinner = (Spinner) findViewById(R.id.SipDefaultProtocolForWifi);
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, SIP_PROTOCOL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String sipWifi = RcsSettings.getInstance().getSipDefaultProtocolForWifi();
        if (sipWifi.equalsIgnoreCase(SIP_PROTOCOL[0])) {
            spinner.setSelection(0);
        } else
        if (sipWifi.equalsIgnoreCase(SIP_PROTOCOL[1])) {
            spinner.setSelection(1);
        } else {
            spinner.setSelection(2);
        }

        String[] certificates = loadCertificatesList();
        spinner = (Spinner) findViewById(R.id.TlsCertificateRoot);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, certificates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        boolean found = false;
        String certRoot = RcsSettings.getInstance().getTlsCertificateRoot();
        for (int i = 0; i < certificates.length; i++) {
        	if (certRoot.contains(certificates[i])) {
                spinner.setSelection(i);
                found = true;
            }
        }
        if (!found) {
            spinner.setSelection(0);
            RcsSettings.getInstance().writeParameter(RcsSettingsData.TLS_CERTIFICATE_ROOT, "");
        }

        spinner = (Spinner) findViewById(R.id.TlsCertificateIntermediate);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, certificates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        found = false;
        String certInt = RcsSettings.getInstance().getTlsCertificateIntermediate();
        for (int i = 0; i < certificates.length; i++) {
            if (certInt.contains(certificates[i])) {
                spinner.setSelection(i);
                found = true;
            }
        }
        if (!found) {
            spinner.setSelection(0);
            RcsSettings.getInstance().writeParameter(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, "");
        }

        spinner = (Spinner)findViewById(R.id.FtProtocol);
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, FT_PROTOCOL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String ftProtocol = RcsSettings.getInstance().getFtProtocol();
        if ((ftProtocol != null) && ftProtocol.equalsIgnoreCase(FT_PROTOCOL[0])) {
            spinner.setSelection(0);
        } else {
            spinner.setSelection(1);
        }

        txt = (EditText)this.findViewById(R.id.ImsServicePollingPeriod);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.IMS_SERVICE_POLLING_PERIOD));

        txt = (EditText)this.findViewById(R.id.SipListeningPort);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SIP_DEFAULT_PORT));

        txt = (EditText)this.findViewById(R.id.SipTimerT1);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SIP_TIMER_T1));

        txt = (EditText)this.findViewById(R.id.SipTimerT2);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SIP_TIMER_T2));

        txt = (EditText)this.findViewById(R.id.SipTimerT4);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SIP_TIMER_T4));

        txt = (EditText)this.findViewById(R.id.SipTransactionTimeout);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SIP_TRANSACTION_TIMEOUT));

        txt = (EditText)this.findViewById(R.id.SipKeepAlivePeriod);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SIP_KEEP_ALIVE_PERIOD));

        txt = (EditText)this.findViewById(R.id.DefaultMsrpPort);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MSRP_DEFAULT_PORT));

	    txt = (EditText)this.findViewById(R.id.DefaultRtpPort);
	    txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.RTP_DEFAULT_PORT));

		txt = (EditText)this.findViewById(R.id.MsrpTransactionTimeout);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.MSRP_TRANSACTION_TIMEOUT));

        txt = (EditText)this.findViewById(R.id.RegisterExpirePeriod);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.REGISTER_EXPIRE_PERIOD));

        txt = (EditText)this.findViewById(R.id.RegisterRetryBaseTime);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.REGISTER_RETRY_BASE_TIME));

        txt = (EditText)this.findViewById(R.id.RegisterRetryMaxTime);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.REGISTER_RETRY_MAX_TIME));

        txt = (EditText)this.findViewById(R.id.PublishExpirePeriod);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.PUBLISH_EXPIRE_PERIOD));

        txt = (EditText)this.findViewById(R.id.RevokeTimeout);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.REVOKE_TIMEOUT));

        txt = (EditText)this.findViewById(R.id.RingingPeriod);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.RINGING_SESSION_PERIOD));

        txt = (EditText)this.findViewById(R.id.SubscribeExpirePeriod);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD));

        txt = (EditText)this.findViewById(R.id.IsComposingTimeout);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.IS_COMPOSING_TIMEOUT));

        txt = (EditText)this.findViewById(R.id.SessionRefreshExpirePeriod);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD));

        txt = (EditText)this.findViewById(R.id.CapabilityRefreshTimeout);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT));

        txt = (EditText)this.findViewById(R.id.CapabilityExpiryTimeout);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT));

        txt = (EditText)this.findViewById(R.id.CapabilityPollingPeriod);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_POLLING_PERIOD));

        check = (CheckBox)this.findViewById(R.id.SipKeepAlive);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.SIP_KEEP_ALIVE)));

        check = (CheckBox)this.findViewById(R.id.PermanentState);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.PERMANENT_STATE_MODE)));

    	check = (CheckBox)this.findViewById(R.id.TelUriFormat);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.TEL_URI_FORMAT)));

    	check = (CheckBox)this.findViewById(R.id.ImAlwaysOn);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.IM_CAPABILITY_ALWAYS_ON)));

    	check = (CheckBox)this.findViewById(R.id.FtAlwaysOn);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.FT_CAPABILITY_ALWAYS_ON)));

    	check = (CheckBox)this.findViewById(R.id.ImUseReports);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.IM_USE_REPORTS)));

        check = (CheckBox)this.findViewById(R.id.Gruu);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.GRUU)));

        check = (CheckBox)this.findViewById(R.id.CpuAlwaysOn);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.CPU_ALWAYS_ON)));

        check = (CheckBox)this.findViewById(R.id.SecureMsrpOverWifi);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.SECURE_MSRP_OVER_WIFI)));

        check = (CheckBox)this.findViewById(R.id.SecureRtpOverWifi);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.SECURE_RTP_OVER_WIFI)));

        check = (CheckBox)this.findViewById(R.id.ImeiAsDeviceId);
        check.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.USE_IMEI_AS_DEVICE_ID)));
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
	        // Save parameters
        	save();
        }
    };
    
    /**
     * Save parameters
     */
    private void save() {
		Spinner spinner = (Spinner)findViewById(R.id.Autoconfig);
		int index = spinner.getSelectedItemPosition();
		switch(index) {
			case 0:
				RcsSettings.getInstance().writeParameter(RcsSettingsData.AUTO_CONFIG_MODE, ""+RcsSettingsData.NO_AUTO_CONFIG);
				break;
			case 1:
				RcsSettings.getInstance().writeParameter(RcsSettingsData.AUTO_CONFIG_MODE, ""+RcsSettingsData.HTTPS_AUTO_CONFIG);
				break;
        }

		spinner = (Spinner)findViewById(R.id.SipDefaultProtocolForMobile);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE, (String)spinner.getSelectedItem());

		spinner = (Spinner)findViewById(R.id.SipDefaultProtocolForWifi);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI, (String)spinner.getSelectedItem());

        spinner = (Spinner) findViewById(R.id.TlsCertificateRoot);
        if (spinner.getSelectedItemPosition() == 0) {
            RcsSettings.getInstance().writeParameter(RcsSettingsData.TLS_CERTIFICATE_ROOT, "");
        } else {
        	String path = CERTIFICATE_FOLDER_PATH + File.separator + (String)spinner.getSelectedItem();
            RcsSettings.getInstance().writeParameter(RcsSettingsData.TLS_CERTIFICATE_ROOT, path);
        }

        spinner = (Spinner) findViewById(R.id.TlsCertificateIntermediate);
        if (spinner.getSelectedItemPosition() == 0) {
            RcsSettings.getInstance().writeParameter(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, "");
        } else {
        	String path = CERTIFICATE_FOLDER_PATH + File.separator + (String)spinner.getSelectedItem();
            RcsSettings.getInstance().writeParameter(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, path);
        }
        
		spinner = (Spinner)findViewById(R.id.NetworkAccess);
		index = spinner.getSelectedItemPosition();
		switch(index) {
			case 0:
				RcsSettings.getInstance().writeParameter(RcsSettingsData.NETWORK_ACCESS, ""+RcsSettingsData.ANY_ACCESS);
				break;
			case 1:
				RcsSettings.getInstance().writeParameter(RcsSettingsData.NETWORK_ACCESS, ""+RcsSettingsData.MOBILE_ACCESS);
				break;
			case 2:
				RcsSettings.getInstance().writeParameter(RcsSettingsData.NETWORK_ACCESS, ""+RcsSettingsData.WIFI_ACCESS);
				break;
        }

        spinner = (Spinner)findViewById(R.id.FtProtocol);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.FT_PROTOCOL, (String)spinner.getSelectedItem());

		EditText txt = (EditText)this.findViewById(R.id.SecondaryProvisioningAddress);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS, txt.getText().toString());

        CheckBox check = (CheckBox)this.findViewById(R.id.SecondaryProvisioningAddressOnly);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY, Boolean.toString(check.isChecked()));

		txt = (EditText)this.findViewById(R.id.SipListeningPort);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_DEFAULT_PORT, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.SipTimerT1);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TIMER_T1, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.SipTimerT2);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TIMER_T2, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.SipTimerT4);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TIMER_T4, txt.getText().toString());

		txt = (EditText)this.findViewById(R.id.SipTransactionTimeout);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TRANSACTION_TIMEOUT, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.SipKeepAlivePeriod);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_KEEP_ALIVE_PERIOD, txt.getText().toString());

		txt = (EditText)this.findViewById(R.id.DefaultMsrpPort);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MSRP_DEFAULT_PORT, txt.getText().toString());

	    txt = (EditText)this.findViewById(R.id.DefaultRtpPort);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.RTP_DEFAULT_PORT, txt.getText().toString());

		txt = (EditText)this.findViewById(R.id.MsrpTransactionTimeout);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.MSRP_TRANSACTION_TIMEOUT, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.RegisterExpirePeriod);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.REGISTER_EXPIRE_PERIOD, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.RegisterRetryBaseTime);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.REGISTER_RETRY_BASE_TIME, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.RegisterRetryMaxTime);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.REGISTER_RETRY_MAX_TIME, txt.getText().toString());

		txt = (EditText)this.findViewById(R.id.PublishExpirePeriod);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.PUBLISH_EXPIRE_PERIOD, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.RevokeTimeout);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.REVOKE_TIMEOUT, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.RingingPeriod);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.RINGING_SESSION_PERIOD, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.SubscribeExpirePeriod);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.IsComposingTimeout);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.IS_COMPOSING_TIMEOUT, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.SessionRefreshExpirePeriod);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.CapabilityRefreshTimeout);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.CapabilityExpiryTimeout);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT, txt.getText().toString());

		txt = (EditText)this.findViewById(R.id.CapabilityPollingPeriod);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_POLLING_PERIOD, txt.getText().toString());
		
    	check = (CheckBox)this.findViewById(R.id.SipKeepAlive);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_KEEP_ALIVE, Boolean.toString(check.isChecked()));

		check = (CheckBox)this.findViewById(R.id.PermanentState);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.PERMANENT_STATE_MODE, Boolean.toString(check.isChecked()));

    	check = (CheckBox)this.findViewById(R.id.TelUriFormat);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.TEL_URI_FORMAT, Boolean.toString(check.isChecked()));

		check = (CheckBox)this.findViewById(R.id.ImAlwaysOn);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.IM_CAPABILITY_ALWAYS_ON, Boolean.toString(check.isChecked()));

		check = (CheckBox)this.findViewById(R.id.FtAlwaysOn);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.FT_CAPABILITY_ALWAYS_ON, Boolean.toString(check.isChecked()));

    	check = (CheckBox)this.findViewById(R.id.ImUseReports);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.IM_USE_REPORTS, Boolean.toString(check.isChecked()));

    	check = (CheckBox)this.findViewById(R.id.Gruu);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.GRUU, Boolean.toString(check.isChecked()));

        check = (CheckBox)this.findViewById(R.id.CpuAlwaysOn);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.CPU_ALWAYS_ON, Boolean.toString(check.isChecked()));

        check = (CheckBox)this.findViewById(R.id.SecureMsrpOverWifi);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.SECURE_MSRP_OVER_WIFI, Boolean.toString(check.isChecked()));

        check = (CheckBox)this.findViewById(R.id.SecureRtpOverWifi);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.SECURE_RTP_OVER_WIFI, Boolean.toString(check.isChecked()));

        check = (CheckBox)this.findViewById(R.id.ImeiAsDeviceId);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.USE_IMEI_AS_DEVICE_ID, Boolean.toString(check.isChecked()));

        Toast.makeText(this, getString(R.string.label_reboot_service), Toast.LENGTH_LONG).show();
	}

    /**
     * Load a list of certificates from the SDCARD
     * 
     * @return List of certificates
     */
    private String[] loadCertificatesList() {
        String[] files = null;
        File folder = new File(CERTIFICATE_FOLDER_PATH);
        try {
            folder.mkdirs();
            if (folder.exists()) {
                // filter
                FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        return filename.contains(RcsSettingsData.CERTIFICATE_FILE_TYPE);
                    }
                };
                files = folder.list(filter);
            }
        } catch (SecurityException e) {
            // intentionally blank
        }
        if (files == null) {
        	// No certificate
            return new String[] {
                getString(R.string.label_no_certificate)
            };
        } else {
        	// Add certificates in the list
        	String[] temp = new String[files.length + 1];
            temp[0] = getString(R.string.label_no_certificate);
            if (files.length > 0) {
                System.arraycopy(files, 0, temp, 1, files.length);
            }
            return temp;
        }
    }
}