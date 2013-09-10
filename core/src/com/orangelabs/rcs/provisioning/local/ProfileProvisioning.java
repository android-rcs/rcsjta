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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
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
 * End user profile parameters provisioning
 *
 * @author jexa7410
 */
public class ProfileProvisioning extends Activity {
	/**
	 * IMS authentication for mobile access
	 */
    private static final String[] MOBILE_IMS_AUTHENT = {
    	RcsSettingsData.GIBA_AUTHENT, RcsSettingsData.DIGEST_AUTHENT
    };

	/**
	 * IMS authentication for Wi-Fi access
	 */
    private static final String[] WIFI_IMS_AUTHENT = {
    	RcsSettingsData.DIGEST_AUTHENT
    };

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.rcs_provisioning_profile);
        
		// Set buttons callback
        Button btn = (Button)findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        btn = (Button)findViewById(R.id.gen_btn);
        btn.setOnClickListener(genBtnListener);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Display parameters
		Spinner spinner = (Spinner)findViewById(R.id.ImsAuhtenticationProcedureForMobile);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, MOBILE_IMS_AUTHENT);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		if (RcsSettings.getInstance().getImsAuhtenticationProcedureForMobile().equals(MOBILE_IMS_AUTHENT[0])) {
			spinner.setSelection(0);
		} else {
			spinner.setSelection(1);
		}

		spinner = (Spinner)findViewById(R.id.ImsAuhtenticationProcedureForWifi);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, WIFI_IMS_AUTHENT);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(0);

		EditText txt = (EditText)this.findViewById(R.id.ImsUsername);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.USERPROFILE_IMS_USERNAME));

		txt = (EditText)this.findViewById(R.id.ImsDisplayName);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME));

		txt = (EditText)this.findViewById(R.id.ImsHomeDomain);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN));

		txt = (EditText)this.findViewById(R.id.ImsPrivateId);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID));

		txt = (EditText)this.findViewById(R.id.ImsPassword);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.USERPROFILE_IMS_PASSWORD));

		txt = (EditText)this.findViewById(R.id.ImsRealm);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.USERPROFILE_IMS_REALM));

        txt = (EditText)this.findViewById(R.id.ImsOutboundProxyAddrForMobile);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.IMS_PROXY_ADDR_MOBILE));

        txt = (EditText)this.findViewById(R.id.ImsOutboundProxyPortForMobile);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.IMS_PROXY_PORT_MOBILE));

		txt = (EditText)this.findViewById(R.id.ImsOutboundProxyAddrForWifi);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.IMS_PROXY_ADDR_WIFI));

		txt = (EditText)this.findViewById(R.id.ImsOutboundProxyPortForWifi);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.IMS_PROXY_PORT_WIFI));

        txt = (EditText)this.findViewById(R.id.XdmServerAddr);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.XDM_SERVER));

		txt = (EditText)this.findViewById(R.id.XdmServerLogin);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.XDM_LOGIN));

		txt = (EditText)this.findViewById(R.id.XdmServerPassword);
        txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.XDM_PASSWORD));
        
		txt = (EditText)this.findViewById(R.id.ImConferenceUri);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.IM_CONF_URI));

        txt = (EditText)this.findViewById(R.id.EndUserConfReqUri);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.ENDUSER_CONFIRMATION_URI));

		txt = (EditText)this.findViewById(R.id.RcsApn);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.RCS_APN));

		txt = (EditText)this.findViewById(R.id.CountryCode);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.COUNTRY_CODE));

		txt = (EditText)this.findViewById(R.id.CountryAreaCode);
		txt.setText(RcsSettings.getInstance().readParameter(RcsSettingsData.COUNTRY_AREA_CODE));

		CheckBox box = (CheckBox)findViewById(R.id.image_sharing);
        box.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_IMAGE_SHARING)));

        box = (CheckBox)findViewById(R.id.video_sharing);
        box.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_VIDEO_SHARING)));

        box = (CheckBox)findViewById(R.id.file_transfer);
        box.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_FILE_TRANSFER)));

        box = (CheckBox)findViewById(R.id.im);
        box.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_IM_SESSION)));
        
        box = (CheckBox)findViewById(R.id.cs_video);
        box.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_CS_VIDEO)));

        box = (CheckBox)findViewById(R.id.presence_discovery);
        box.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY)));

        box = (CheckBox)findViewById(R.id.social_presence);
        box.setChecked(Boolean.parseBoolean(RcsSettings.getInstance().readParameter(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE)));
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
		Spinner spinner = (Spinner)findViewById(R.id.ImsAuhtenticationProcedureForMobile);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE, (String)spinner.getSelectedItem());

		spinner = (Spinner)findViewById(R.id.ImsAuhtenticationProcedureForWifi);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI, (String)spinner.getSelectedItem());

		EditText txt = (EditText)this.findViewById(R.id.ImsUsername);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.USERPROFILE_IMS_USERNAME, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImsDisplayName);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImsHomeDomain);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImsPrivateId);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImsPassword);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.USERPROFILE_IMS_PASSWORD, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImsRealm);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.USERPROFILE_IMS_REALM, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImsOutboundProxyAddrForMobile);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.IMS_PROXY_ADDR_MOBILE, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImsOutboundProxyPortForMobile);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.IMS_PROXY_PORT_MOBILE, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImsOutboundProxyAddrForWifi);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.IMS_PROXY_ADDR_WIFI, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImsOutboundProxyPortForWifi);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.IMS_PROXY_PORT_WIFI, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.XdmServerAddr);
		RcsSettings.getInstance().writeParameter(RcsSettingsData.XDM_SERVER, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.XdmServerLogin);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.XDM_LOGIN, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.XdmServerPassword);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.XDM_PASSWORD, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.ImConferenceUri);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.IM_CONF_URI, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.EndUserConfReqUri);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.ENDUSER_CONFIRMATION_URI, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.RcsApn);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.RCS_APN, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.CountryCode);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.COUNTRY_CODE, txt.getText().toString());

        txt = (EditText)this.findViewById(R.id.CountryAreaCode);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.COUNTRY_AREA_CODE, txt.getText().toString());

        // Save capabilities
        CheckBox box = (CheckBox)findViewById(R.id.image_sharing);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_IMAGE_SHARING, Boolean.toString(box.isChecked()));

        box = (CheckBox)findViewById(R.id.video_sharing);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_VIDEO_SHARING, Boolean.toString(box.isChecked()));

        box = (CheckBox)findViewById(R.id.file_transfer);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_FILE_TRANSFER, Boolean.toString(box.isChecked()));

        box = (CheckBox)findViewById(R.id.im);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_IM_SESSION, Boolean.toString(box.isChecked()));
        
        box = (CheckBox)findViewById(R.id.cs_video);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_CS_VIDEO, Boolean.toString(box.isChecked()));

        box = (CheckBox)findViewById(R.id.presence_discovery);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY, Boolean.toString(box.isChecked()));

        box = (CheckBox)findViewById(R.id.social_presence);
        RcsSettings.getInstance().writeParameter(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, Boolean.toString(box.isChecked()));

        Toast.makeText(this, getString(R.string.label_reboot_service), Toast.LENGTH_LONG).show();    	
    }
    
    /**
     * Generate profile button listener
     */
    private OnClickListener genBtnListener = new OnClickListener() {
        public void onClick(View v) {
	        // Save parameters
        	genProfile();
        }
    };
    
    /**
     * Generate the user profile
     */
    private void genProfile() {    
		LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.rcs_provisioning_generate_profile, null);
		EditText textEdit = (EditText)view.findViewById(R.id.msisdn);
        textEdit.setText(RcsSettings.getInstance().getCountryCode());

        final String[] platforms = {
        		"NSN Lannion", "NSN Brune", "Margaux (albatros)", "Margaux (blackbird)", "VCOM1", "VCOM2",
                "RCS", "Kamailio1", "MargauxIPv6", "Huawei", "Capgemini", "JibeNet"
        };
        Spinner spinner = (Spinner)view.findViewById(R.id.ims);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, platforms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.label_generate_profile)
            .setView(view)
            .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
        	        // Generate default settings
        			EditText textEdit = (EditText)view.findViewById(R.id.msisdn);
        			String number = textEdit.getText().toString();
    	            Spinner spinner = (Spinner)view.findViewById(R.id.ims);
    	            int index = spinner.getSelectedItemPosition();

        			String sipUri = "";
        			String homeDomain = "";
                    String privateSipUri = "";
        			String imsPwd = "";
        			String imsRealm = "";
        			String imsAddrForMobile = "";
        			int imsPortForMobile = 5060;
                    String imsAddrForWifi = "";
                    int imsPortForWifi = 5060;
        			String xdms = "";
        			String xdmsPwd = "";
        			String xdmsLogin = "";
					String ftHttpServerAddr = "";
					String ftHttpServerLogin = "";
					String ftHttpServerPwd = "";
        			String confUri = "";
        			String enduserConfirmUri = "";
                    switch(index) {
	                	case 0: // NSN Lannion
	            			homeDomain = "rcs.lannion.com";
	            			sipUri = number + "@" + homeDomain;
	        				privateSipUri = sipUri;
	            			imsPwd = "alu2012";
	            			imsRealm = "rcs.lannion.com";
	            			imsAddrForMobile = "80.12.197.184";
	            			imsPortForMobile = 5080;
	            			imsAddrForWifi = "80.12.197.184";
	            			imsPortForWifi = 5080;
	            			confUri = "sip:Conference-Factory@" + homeDomain;
	            			break;
                    	case 1: // NSN Brune
	            			homeDomain = "rcs.brune.com";
            				sipUri = number + "@" + homeDomain;
            				privateSipUri = sipUri;
	            			imsPwd = "nsnims2008";
	            			imsRealm = "rcs.brune.com";
	            			imsAddrForMobile = "80.12.197.74";
	            			imsPortForMobile = 5060;
	            			imsAddrForWifi = "80.12.197.74";
	            			imsPortForWifi = 5060;
	            			confUri = "sip:Conference-Factory@" + homeDomain;
	            			break;
                        case 2: // Margaux (albatros)
                            homeDomain = "sip.mobistar.com";
                            sipUri = number + "@" + homeDomain;
            				privateSipUri = sipUri;
                            imsPwd = "imt30imt30";
	            			imsRealm = "sip.mobistar.com";
	            			imsAddrForMobile = "sip.mobistar.com";
	            			imsPortForMobile = 5060;
	            			imsAddrForWifi = "sip.mobistar.com";
	            			imsPortForWifi = 5060;
	            			confUri  = "sip:Conference-Factory@" + homeDomain;
	            			ftHttpServerAddr = "";
	            			ftHttpServerLogin = "";
	            			ftHttpServerPwd = "";
                            break;
                        case 3: // Margaux (blackbird)
                            homeDomain = "rcs.lannion-e.com";
                            sipUri = number + "@" + homeDomain;
                            privateSipUri = sipUri;
                            imsPwd = "imt30imt30";
                            imsRealm = "sip.mobistar.com";
                            imsAddrForMobile = "172.20.84.114";
                            imsPortForMobile = 5080;
                            imsAddrForWifi = "172.20.84.114";
                            imsPortForWifi = 5080;
                            confUri  = "sip:Conference-Factory@" + homeDomain;
                            ftHttpServerAddr = "https://172.20.65.52/rcse-hcs/upload";
                            ftHttpServerLogin = sipUri;
                            ftHttpServerPwd = "imt30imt30";
                            break;
                        case 4: // VCO1
                            homeDomain = "sip.france.fr";
                            sipUri = number + "@" + homeDomain;
            				privateSipUri = sipUri;
                            imsPwd = "imt30imt30";
	            			imsRealm = "sip.france.fr";
	            			imsAddrForMobile = "asbc.sip.france.fr";
	            			imsPortForMobile = 5080;
	            			imsAddrForWifi = "asbc.sip.france.fr";
	            			imsPortForWifi = 5080;
	            			confUri  = "sip:Conference-Factory@" + homeDomain;
                            break;
                        case 5: // VCO2
                            homeDomain = "sip.france.fr";
                            sipUri = number + "@" + homeDomain;
            				privateSipUri = sipUri;
                            imsPwd = "imt30imt30";
	            			imsRealm = "sip.france.fr";
	            			imsAddrForMobile = "172.20.114.42";
	            			imsPortForMobile = 5060;
	            			imsAddrForWifi = "172.20.114.42";
	            			imsPortForWifi = 5060;
	            			confUri  = "sip:Conference-Factory@" + homeDomain;
                            break;
                        case 6: // RCS
                            homeDomain = "sip.france.fr";
                            sipUri = number + "@" + homeDomain;
            				privateSipUri = sipUri;
                            imsPwd = "imt30imt30";
	            			imsRealm = "sip.france.fr";
	            			imsAddrForMobile = "172.20.84.114";
	            			imsPortForMobile = 5060;
	            			imsAddrForWifi = "172.20.84.114";
	            			imsPortForWifi = 5060;
	            			confUri  = "sip:Conference-Factory@" + homeDomain;
                            break;
                        case 7: // Kamailio1
                            homeDomain = "rcs.kamailio1.com";
                            sipUri = number + "@" + homeDomain;
            				privateSipUri = sipUri;
                            imsPwd = "password";
	            			imsRealm = "rcs.kamailio1.com";
	            			imsAddrForMobile = "172.20.14.43";
	            			imsPortForMobile = 5060;
	            			imsAddrForWifi = "172.20.14.43";
	            			imsPortForWifi = 5060;
	            			confUri  = "sip:Conference-Factory@" + homeDomain;
                            break;
                        case 8: // Margaux IPv6
                            homeDomain = "sip.mobistar.com";
                            sipUri = number + "@" + homeDomain;
            				privateSipUri = sipUri;
                            imsPwd = "imt30imt30";
	            			imsRealm = "sip.mobistar.com";
                            imsAddrForMobile = "2a01:cf00:74:410f::14";
                            imsPortForMobile = 5060;
                            imsAddrForWifi = "2a01:cf00:74:410f::14";
                            imsPortForWifi = 5060;
                            confUri  = "sip:Conference-Factory@" + homeDomain;
                            break;
                        case 9: // Huawei
                            homeDomain = "sip.osk.com";
                            sipUri = number + "@" + homeDomain;
                            if (sipUri.startsWith("+")) {
                            	privateSipUri = sipUri.substring(1);
                            } else {
                            	privateSipUri = sipUri;
                            }
                            imsPwd = "huawei";
	            			imsRealm = "sip.osk.com";
                            imsAddrForMobile = "172.20.114.0";
                            imsPortForMobile = 5060;
                            imsAddrForWifi = "172.20.114.0";
                            imsPortForWifi = 5060;
                            confUri  = "sip:Conference-Factory@" + homeDomain;
                            break;
                        case 10: // Capgemini
                            homeDomain = "sims2.net";
                            sipUri = number + "@" + homeDomain;
            				privateSipUri = sipUri;
                            imsPwd = "1234";
	            			imsRealm = "sims2.net";
                            imsAddrForMobile = "10.67.102.151";
                            imsPortForMobile = 5060;
                            imsAddrForWifi = "10.67.102.151";
                            imsPortForWifi = 5060;
                            confUri  = "sip:Conference-Factory@" + homeDomain;
                            break;
                        case 11: // JibeNet
                            homeDomain = "jibemobile.com";
                            sipUri = number + "@" + homeDomain;
            				privateSipUri = sipUri;
                            imsPwd = "5555";
	            			imsRealm = "jibemobile.com";
                            imsAddrForMobile = "goose.jibemobile.com";
                            imsPortForMobile = 5671;
                            imsAddrForWifi = "goose.jibemobile.com";
                            imsPortForWifi = 5671;
                            confUri  = "sip:conference@" + homeDomain;
                            break;
                    }

        			// Update UI
    				EditText txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsUsername);
    				txt.setText(number);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsDisplayName);
    				txt.setText(number);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsHomeDomain);
    		        txt.setText(homeDomain);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsPrivateId);
                    txt.setText(privateSipUri);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsPassword);
    		        txt.setText(imsPwd);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsRealm);
    		        txt.setText(imsRealm);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsOutboundProxyAddrForMobile);
                    txt.setText(imsAddrForMobile);
			        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsOutboundProxyPortForMobile);
		            txt.setText(""+imsPortForMobile);
                    txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsOutboundProxyAddrForWifi);
                    txt.setText(imsAddrForWifi);
                    txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImsOutboundProxyPortForWifi);
                    txt.setText(""+imsPortForWifi);
    				txt = (EditText)ProfileProvisioning.this.findViewById(R.id.XdmServerAddr);
    				txt.setText(xdms);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.XdmServerLogin);
    		        txt.setText(xdmsLogin);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.XdmServerPassword);
    		        txt.setText(xdmsPwd);
    				txt = (EditText)ProfileProvisioning.this.findViewById(R.id.FtHttpServerAddr);
    				txt.setText(ftHttpServerAddr);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.FtHttpServerLogin);
    		        txt.setText(ftHttpServerLogin);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.FtHttpServerPassword);
    		        txt.setText(ftHttpServerPwd);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.ImConferenceUri);
    		        txt.setText(confUri);
    		        txt = (EditText)ProfileProvisioning.this.findViewById(R.id.EndUserConfReqUri);
    		        txt.setText(enduserConfirmUri);
    	        }
            }).setNegativeButton(R.string.label_cancel, null);
        
        AlertDialog alert = builder.create();
    	alert.show();
    }
}
