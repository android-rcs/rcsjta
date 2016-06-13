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
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.gsma.rcs.utils.logger.Logger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * End user profile parameters provisioning
 *
 * @author Philippe LEMORDANT
 */
public class ProfileProvisioning extends Fragment implements IProvisioningFragment {

    private static final Logger sLogger = Logger.getLogger(ProfileProvisioning.class.getName());

    /**
     * IMS authentication for mobile access
     */
    private static final String[] MOBILE_IMS_AUTHENT = {
            AuthenticationProcedure.GIBA.name(), AuthenticationProcedure.DIGEST.name()
    };

    private static RcsSettings sRcsSettings;
    private View mRootView;
    private ProvisioningHelper mHelper;

    public static ProfileProvisioning newInstance(RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.debug("new instance");
        }
        ProfileProvisioning f = new ProfileProvisioning();
        /*
         * If Android decides to recreate your Fragment later, it's going to call the no-argument
         * constructor of your fragment. So overloading the constructor is not a solution. A way to
         * pass argument to new fragment is to store it as static.
         */
        sRcsSettings = rcsSettings;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.provisioning_profile, container, false);
        mHelper = new ProvisioningHelper(mRootView, sRcsSettings);
        displayRcsSettings();
        return mRootView;
    }

    @Override
    public void displayRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("displayRcsSettings");
        }
        Spinner spinner = (Spinner) mRootView
                .findViewById(R.id.ImsAuthenticationProcedureForMobile);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, MOBILE_IMS_AUTHENT);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(sRcsSettings.getImsAuthenticationProcedureForMobile().toInt());

        mHelper.setContactIdEditText(R.id.ImsUsername, RcsSettingsData.USERPROFILE_IMS_USERNAME);
        mHelper.setStringEditText(R.id.ImsDisplayName, RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME);
        mHelper.setStringEditText(R.id.ImsHomeDomain, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN);
        mHelper.setStringEditText(R.id.ImsPrivateId, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID);
        mHelper.setStringEditText(R.id.ImsPassword, RcsSettingsData.USERPROFILE_IMS_PASSWORD);
        mHelper.setStringEditText(R.id.ImsRealm, RcsSettingsData.USERPROFILE_IMS_REALM);
        mHelper.setStringEditText(R.id.ImsOutboundProxyAddrForMobile,
                RcsSettingsData.IMS_PROXY_ADDR_MOBILE);
        mHelper.setIntEditText(R.id.ImsOutboundProxyPortForMobile,
                RcsSettingsData.IMS_PROXY_PORT_MOBILE);
        mHelper.setStringEditText(R.id.ImsOutboundProxyAddrForWifi,
                RcsSettingsData.IMS_PROXY_ADDR_WIFI);
        mHelper.setIntEditText(R.id.ImsOutboundProxyPortForWifi,
                RcsSettingsData.IMS_PROXY_PORT_WIFI);
        mHelper.setUriEditText(R.id.XdmServerAddr, RcsSettingsData.XDM_SERVER);
        mHelper.setStringEditText(R.id.XdmServerLogin, RcsSettingsData.XDM_LOGIN);
        mHelper.setStringEditText(R.id.XdmServerPassword, RcsSettingsData.XDM_PASSWORD);
        mHelper.setUriEditText(R.id.FtHttpServerAddr, RcsSettingsData.FT_HTTP_SERVER);
        mHelper.setStringEditText(R.id.FtHttpServerLogin, RcsSettingsData.FT_HTTP_LOGIN);
        mHelper.setStringEditText(R.id.FtHttpServerPassword, RcsSettingsData.FT_HTTP_PASSWORD);
        mHelper.setUriEditText(R.id.ImConferenceUri, RcsSettingsData.IM_CONF_URI);
        mHelper.setUriEditText(R.id.EndUserConfReqUri, RcsSettingsData.ENDUSER_CONFIRMATION_URI);
        mHelper.setStringEditText(R.id.RcsApn, RcsSettingsData.RCS_APN);

        TextView txt = (TextView) mRootView.findViewById(R.id.release);
        txt.setText(sRcsSettings.getGsmaRelease().name());
        txt = (TextView) mRootView.findViewById(R.id.user_msg_title);
        String title = sRcsSettings.getProvisioningUserMessageTitle();
        if (title != null) {
            txt.setText(title);
        }
        txt = (TextView) mRootView.findViewById(R.id.user_msg_content);
        String content = sRcsSettings.getProvisioningUserMessageContent();
        if (content != null) {
            txt.setText(content);
        }
    }

    @Override
    public void persistRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("persistRcsSettings");
        }
        Spinner spinner = (Spinner) mRootView
                .findViewById(R.id.ImsAuthenticationProcedureForMobile);
        AuthenticationProcedure procedure = AuthenticationProcedure.valueOf((String) spinner
                .getSelectedItem());
        sRcsSettings.setImsAuthenticationProcedureForMobile(procedure);

        mHelper.saveContactIdEditText(R.id.ImsUsername, RcsSettingsData.USERPROFILE_IMS_USERNAME);
        mHelper.saveStringEditText(R.id.ImsDisplayName,
                RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME);
        mHelper.saveStringEditText(R.id.ImsHomeDomain, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN);
        mHelper.saveStringEditText(R.id.ImsPrivateId, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID);
        mHelper.saveStringEditText(R.id.ImsPassword, RcsSettingsData.USERPROFILE_IMS_PASSWORD);
        mHelper.saveStringEditText(R.id.ImsRealm, RcsSettingsData.USERPROFILE_IMS_REALM);
        mHelper.saveStringEditText(R.id.ImsOutboundProxyAddrForMobile,
                RcsSettingsData.IMS_PROXY_ADDR_MOBILE);
        mHelper.saveIntEditText(R.id.ImsOutboundProxyPortForMobile,
                RcsSettingsData.IMS_PROXY_PORT_MOBILE);
        mHelper.saveStringEditText(R.id.ImsOutboundProxyAddrForWifi,
                RcsSettingsData.IMS_PROXY_ADDR_WIFI);
        mHelper.saveIntEditText(R.id.ImsOutboundProxyPortForWifi,
                RcsSettingsData.IMS_PROXY_PORT_WIFI);
        mHelper.saveUriEditText(R.id.XdmServerAddr, RcsSettingsData.XDM_SERVER);
        mHelper.saveStringEditText(R.id.XdmServerLogin, RcsSettingsData.XDM_LOGIN);
        mHelper.saveStringEditText(R.id.XdmServerPassword, RcsSettingsData.XDM_PASSWORD);
        mHelper.saveUriEditText(R.id.FtHttpServerAddr, RcsSettingsData.FT_HTTP_SERVER);
        mHelper.saveStringEditText(R.id.FtHttpServerLogin, RcsSettingsData.FT_HTTP_LOGIN);
        mHelper.saveStringEditText(R.id.FtHttpServerPassword, RcsSettingsData.FT_HTTP_PASSWORD);

        sRcsSettings.setFileTransferHttpSupported(sRcsSettings.getFtHttpServer() != null
                && sRcsSettings.getFtHttpLogin() != null
                && sRcsSettings.getFtHttpPassword() != null);

        mHelper.saveUriEditText(R.id.ImConferenceUri, RcsSettingsData.IM_CONF_URI);
        mHelper.saveUriEditText(R.id.EndUserConfReqUri, RcsSettingsData.ENDUSER_CONFIRMATION_URI);
        mHelper.saveStringEditText(R.id.RcsApn, RcsSettingsData.RCS_APN);
    }

}
