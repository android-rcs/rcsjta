/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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

import com.gsma.rcs.R;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.provider.settings.RcsSettingsData.ConfigurationMode;
import com.gsma.rcs.provider.settings.RcsSettingsData.EnableRcseSwitch;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.provider.settings.RcsSettingsData.NetworkAccessType;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Stack parameters provisioning File
 * 
 * @author jexa7410
 */
public class StackProvisioning extends Fragment implements IProvisioningFragment {

    private static final Logger sLogger = Logger.getLogger(StackProvisioning.class.getName());

    /**
     * Folder path for certificate
     */
    public static final String CERTIFICATE_FOLDER_PATH = Environment.getExternalStorageDirectory()
            .getPath();

    /**
     * Configuration modes
     */
    private String[] mConfigModes;

    /**
     * SIP protocol
     */
    private static final String[] SIP_PROTOCOL = {
            "UDP", "TCP", "TLS"
    };

    /**
     * Enable RCS switch
     */
    private String[] mEnableRcseSwitch;

    /**
     * Network accesses
     */
    private String[] mNetworkAccesses;

    /**
     * FT protocol
     */
    private static final String[] FT_PROTOCOL = {
            FileTransferProtocol.HTTP.name(), FileTransferProtocol.MSRP.name()
    };

    private static RcsSettings sRcsSettings;
    private View mRootView;
    private ProvisioningHelper mHelper;

    public static StackProvisioning newInstance(RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.debug("new instance");
        }
        StackProvisioning f = new StackProvisioning();
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
        mRootView = inflater.inflate(R.layout.provisioning_stack, container, false);
        mHelper = new ProvisioningHelper(mRootView, sRcsSettings);
        mConfigModes = getResources().getStringArray(R.array.provisioning_config_mode);
        mNetworkAccesses = getResources().getStringArray(R.array.provisioning_network_access);
        mEnableRcseSwitch = getResources().getStringArray(R.array.provisioning_enable_rcs_switch);
        displayRcsSettings();
        return mRootView;
    }

    @Override
    public void displayRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("displayRcsSettings");
        }
        Context ctx = getContext();
        Spinner spinner = (Spinner) mRootView.findViewById(R.id.Autoconfig);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_spinner_item, mConfigModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        ConfigurationMode mode = sRcsSettings.getConfigurationMode();
        spinner.setSelection(ConfigurationMode.AUTO.equals(mode) ? 1 : 0);

        TextView textView = (TextView) mRootView.findViewById(R.id.client_vendor);
        textView.setText(Build.MANUFACTURER);

        spinner = (Spinner) mRootView.findViewById(R.id.EnableRcsSwitch);
        adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, mEnableRcseSwitch);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        EnableRcseSwitch rcsSwitch = sRcsSettings.getEnableRcseSwitch();
        switch (rcsSwitch) {
            case ALWAYS_SHOW:
            case ONLY_SHOW_IN_ROAMING:
                spinner.setSelection(rcsSwitch.toInt());
                break;
            default:
                spinner.setSelection(2);
        }
        mHelper.setStringEditText(R.id.SecondaryProvisioningAddress,
                RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS);
        mHelper.setBoolCheckBox(R.id.SecondaryProvisioningAddressOnly,
                RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY);

        spinner = (Spinner) mRootView.findViewById(R.id.NetworkAccess);
        adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, mNetworkAccesses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        NetworkAccessType access = sRcsSettings.getNetworkAccess();
        switch (access) {
            case MOBILE:
                spinner.setSelection(1);
                break;
            case WIFI:
                spinner.setSelection(2);
                break;
            case ANY:
            default:
                spinner.setSelection(0);
        }
        spinner = (Spinner) mRootView.findViewById(R.id.SipDefaultProtocolForMobile);
        adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, SIP_PROTOCOL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String sipMobile = sRcsSettings.getSipDefaultProtocolForMobile();
        if (SIP_PROTOCOL[0].equalsIgnoreCase(sipMobile)) {
            spinner.setSelection(0);
        } else if (SIP_PROTOCOL[1].equalsIgnoreCase(sipMobile)) {
            spinner.setSelection(1);
        } else {
            spinner.setSelection(2);
        }
        spinner = (Spinner) mRootView.findViewById(R.id.SipDefaultProtocolForWifi);
        adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, SIP_PROTOCOL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String sipWifi = sRcsSettings.getSipDefaultProtocolForWifi();
        if (SIP_PROTOCOL[0].equalsIgnoreCase(sipWifi)) {
            spinner.setSelection(0);
        } else if (SIP_PROTOCOL[1].equalsIgnoreCase(sipWifi)) {
            spinner.setSelection(1);
        } else {
            spinner.setSelection(2);
        }
        String[] certificates = loadCertificatesList();
        spinner = (Spinner) mRootView.findViewById(R.id.TlsCertificateRoot);
        adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, certificates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        boolean found = false;
        String certRoot = sRcsSettings.getTlsCertificateRoot();
        for (int i = 0; i < certificates.length; i++) {
            if (certRoot != null && certRoot.contains(certificates[i])) {
                spinner.setSelection(i);
                found = true;
            }
        }
        if (!found) {
            spinner.setSelection(0);
            sRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_ROOT, "");
        }
        spinner = (Spinner) mRootView.findViewById(R.id.TlsCertificateIntermediate);
        adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, certificates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        found = false;
        String certInt = sRcsSettings.getTlsCertificateIntermediate();
        for (int i = 0; i < certificates.length; i++) {
            if (certInt != null && certInt.contains(certificates[i])) {
                spinner.setSelection(i);
                found = true;
            }
        }
        if (!found) {
            spinner.setSelection(0);
            sRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, "");
        }
        spinner = (Spinner) mRootView.findViewById(R.id.FtProtocol);
        adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item, FT_PROTOCOL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        FileTransferProtocol ftProtocol = sRcsSettings.getFtProtocol();
        if (FileTransferProtocol.HTTP.equals(ftProtocol)) {
            spinner.setSelection(0);
        } else {
            spinner.setSelection(1);
        }
        mHelper.setLongEditText(R.id.ImsServicePollingPeriod,
                RcsSettingsData.IMS_SERVICE_POLLING_PERIOD);
        mHelper.setIntEditText(R.id.SipListeningPort, RcsSettingsData.SIP_DEFAULT_PORT);
        mHelper.setLongEditText(R.id.SipTimerT1, RcsSettingsData.SIP_TIMER_T1);
        mHelper.setLongEditText(R.id.SipTimerT2, RcsSettingsData.SIP_TIMER_T2);
        mHelper.setLongEditText(R.id.SipTimerT4, RcsSettingsData.SIP_TIMER_T4);
        mHelper.setLongEditText(R.id.SipTransactionTimeout, RcsSettingsData.SIP_TRANSACTION_TIMEOUT);
        mHelper.setLongEditText(R.id.SipKeepAlivePeriod, RcsSettingsData.SIP_KEEP_ALIVE_PERIOD);
        mHelper.setIntEditText(R.id.DefaultMsrpPort, RcsSettingsData.MSRP_DEFAULT_PORT);
        mHelper.setIntEditText(R.id.DefaultRtpPort, RcsSettingsData.RTP_DEFAULT_PORT);
        mHelper.setLongEditText(R.id.MsrpTransactionTimeout,
                RcsSettingsData.MSRP_TRANSACTION_TIMEOUT);
        mHelper.setLongEditText(R.id.RegisterExpirePeriod, RcsSettingsData.REGISTER_EXPIRE_PERIOD);
        mHelper.setLongEditText(R.id.RegisterRetryBaseTime,
                RcsSettingsData.REGISTER_RETRY_BASE_TIME);
        mHelper.setLongEditText(R.id.RegisterRetryMaxTime, RcsSettingsData.REGISTER_RETRY_MAX_TIME);
        mHelper.setLongEditText(R.id.PublishExpirePeriod, RcsSettingsData.PUBLISH_EXPIRE_PERIOD);
        mHelper.setLongEditText(R.id.RevokeTimeout, RcsSettingsData.REVOKE_TIMEOUT);
        mHelper.setLongEditText(R.id.RingingPeriod, RcsSettingsData.RINGING_SESSION_PERIOD);
        mHelper.setLongEditText(R.id.SubscribeExpirePeriod, RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD);
        mHelper.setLongEditText(R.id.IsComposingTimeout, RcsSettingsData.IS_COMPOSING_TIMEOUT);
        mHelper.setLongEditText(R.id.SessionRefreshExpirePeriod,
                RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD);
        mHelper.setLongEditText(R.id.CapabilityRefreshTimeout,
                RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT);
        mHelper.setLongEditText(R.id.CapabilityExpiryTimeout,
                RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT);
        mHelper.setLongEditText(R.id.CapabilityPollingPeriod,
                RcsSettingsData.CAPABILITY_POLLING_PERIOD);
        mHelper.setBoolCheckBox(R.id.TcpFallback, RcsSettingsData.TCP_FALLBACK);
        mHelper.setBoolCheckBox(R.id.SipKeepAlive, RcsSettingsData.SIP_KEEP_ALIVE);
        mHelper.setBoolCheckBox(R.id.PermanentState, RcsSettingsData.PERMANENT_STATE_MODE);
        mHelper.setBoolCheckBox(R.id.TelUriFormat, RcsSettingsData.TEL_URI_FORMAT);
        mHelper.setBoolCheckBox(R.id.ImAlwaysOn, RcsSettingsData.IM_CAPABILITY_ALWAYS_ON);
        mHelper.setBoolCheckBox(R.id.FtAlwaysOn, RcsSettingsData.FT_CAPABILITY_ALWAYS_ON);
        mHelper.setBoolCheckBox(R.id.FtHttpAlwaysOn, RcsSettingsData.FT_HTTP_CAP_ALWAYS_ON);
        mHelper.setBoolCheckBox(R.id.InviteOnlyGroupchatSF,
                RcsSettingsData.GROUP_CHAT_INVITE_ONLY_FULL_SF);
        mHelper.setBoolCheckBox(R.id.ImUseReports, RcsSettingsData.IM_USE_REPORTS);
        mHelper.setBoolCheckBox(R.id.Gruu, RcsSettingsData.GRUU);
        mHelper.setBoolCheckBox(R.id.CpuAlwaysOn, RcsSettingsData.CPU_ALWAYS_ON);
        mHelper.setBoolCheckBox(R.id.SecureMsrpOverWifi, RcsSettingsData.SECURE_MSRP_OVER_WIFI);
        mHelper.setBoolCheckBox(R.id.SecureRtpOverWifi, RcsSettingsData.SECURE_RTP_OVER_WIFI);
        mHelper.setBoolCheckBox(R.id.ImeiAsDeviceId, RcsSettingsData.USE_IMEI_AS_DEVICE_ID);
        mHelper.setBoolCheckBox(R.id.ControlExtensions, RcsSettingsData.CONTROL_EXTENSIONS);
        mHelper.setBoolCheckBox(R.id.AllowExtensions, RcsSettingsData.ALLOW_EXTENSIONS);
        mHelper.setIntEditText(R.id.MaxMsrpLengthExtensions,
                RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS);
        mHelper.setLongEditText(R.id.MessagingCapabilitiesValidity,
                RcsSettingsData.MSG_CAP_VALIDITY_PERIOD);
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
            // noinspection ResultOfMethodCallIgnored
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
        }
        // Add certificates in the list
        String[] temp = new String[files.length + 1];
        temp[0] = getString(R.string.label_no_certificate);
        if (files.length > 0) {
            System.arraycopy(files, 0, temp, 1, files.length);
        }
        return temp;
    }

    @Override
    public void persistRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("persistRcsSettings");
        }
        Spinner spinner = (Spinner) mRootView.findViewById(R.id.Autoconfig);
        switch (spinner.getSelectedItemPosition()) {
            case 0:
                sRcsSettings.setConfigurationMode(ConfigurationMode.MANUAL);
                break;
            default:
                sRcsSettings.setConfigurationMode(ConfigurationMode.AUTO);
                break;
        }
        spinner = (Spinner) mRootView.findViewById(R.id.SipDefaultProtocolForMobile);
        sRcsSettings.writeString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
                (String) spinner.getSelectedItem());

        spinner = (Spinner) mRootView.findViewById(R.id.EnableRcsSwitch);
        switch (spinner.getSelectedItemPosition()) {
            case 1:
                sRcsSettings.setEnableRcseSwitch(EnableRcseSwitch.ALWAYS_SHOW);
                break;
            case 0:
                sRcsSettings.setEnableRcseSwitch(EnableRcseSwitch.ONLY_SHOW_IN_ROAMING);
                break;
            default:
                sRcsSettings.setEnableRcseSwitch(EnableRcseSwitch.NEVER_SHOW);
        }
        spinner = (Spinner) mRootView.findViewById(R.id.SipDefaultProtocolForWifi);
        sRcsSettings.writeString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
                (String) spinner.getSelectedItem());

        mHelper.saveBoolCheckBox(R.id.TcpFallback, RcsSettingsData.TCP_FALLBACK);

        spinner = (Spinner) mRootView.findViewById(R.id.TlsCertificateRoot);
        if (spinner.getSelectedItemPosition() == 0) {
            sRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_ROOT, null);
        } else {
            String path = CERTIFICATE_FOLDER_PATH + File.separator + spinner.getSelectedItem();
            sRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_ROOT, path);
        }
        spinner = (Spinner) mRootView.findViewById(R.id.TlsCertificateIntermediate);
        if (spinner.getSelectedItemPosition() == 0) {
            sRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, null);
        } else {
            String path = CERTIFICATE_FOLDER_PATH + File.separator + spinner.getSelectedItem();
            sRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, path);
        }
        spinner = (Spinner) mRootView.findViewById(R.id.NetworkAccess);
        switch (spinner.getSelectedItemPosition()) {
            case 1:
                sRcsSettings.setNetworkAccess(NetworkAccessType.MOBILE);
                break;
            case 2:
                sRcsSettings.setNetworkAccess(NetworkAccessType.WIFI);
                break;
            default:
                sRcsSettings.setNetworkAccess(NetworkAccessType.ANY);
        }
        spinner = (Spinner) mRootView.findViewById(R.id.FtProtocol);
        FileTransferProtocol protocol = FileTransferProtocol.valueOf((String) spinner
                .getSelectedItem());
        sRcsSettings.setFtProtocol(protocol);

        mHelper.saveStringEditText(R.id.SecondaryProvisioningAddress,
                RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS);
        mHelper.saveBoolCheckBox(R.id.SecondaryProvisioningAddressOnly,
                RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY);
        mHelper.saveLongEditText(R.id.ImsServicePollingPeriod,
                RcsSettingsData.IMS_SERVICE_POLLING_PERIOD);
        mHelper.saveIntEditText(R.id.SipListeningPort, RcsSettingsData.SIP_DEFAULT_PORT);
        mHelper.saveLongEditText(R.id.SipTimerT1, RcsSettingsData.SIP_TIMER_T1);
        mHelper.saveLongEditText(R.id.SipTimerT2, RcsSettingsData.SIP_TIMER_T2);
        mHelper.saveLongEditText(R.id.SipTimerT4, RcsSettingsData.SIP_TIMER_T4);
        mHelper.saveLongEditText(R.id.SipTransactionTimeout,
                RcsSettingsData.SIP_TRANSACTION_TIMEOUT);
        mHelper.saveLongEditText(R.id.SipKeepAlivePeriod, RcsSettingsData.SIP_KEEP_ALIVE_PERIOD);
        mHelper.saveIntEditText(R.id.DefaultMsrpPort, RcsSettingsData.MSRP_DEFAULT_PORT);
        mHelper.saveIntEditText(R.id.DefaultRtpPort, RcsSettingsData.RTP_DEFAULT_PORT);
        mHelper.saveLongEditText(R.id.MsrpTransactionTimeout,
                RcsSettingsData.MSRP_TRANSACTION_TIMEOUT);
        mHelper.saveLongEditText(R.id.RegisterExpirePeriod, RcsSettingsData.REGISTER_EXPIRE_PERIOD);
        mHelper.saveLongEditText(R.id.RegisterRetryBaseTime,
                RcsSettingsData.REGISTER_RETRY_BASE_TIME);
        mHelper.saveLongEditText(R.id.RegisterRetryMaxTime, RcsSettingsData.REGISTER_RETRY_MAX_TIME);
        mHelper.saveLongEditText(R.id.PublishExpirePeriod, RcsSettingsData.PUBLISH_EXPIRE_PERIOD);
        mHelper.saveLongEditText(R.id.RevokeTimeout, RcsSettingsData.REVOKE_TIMEOUT);
        mHelper.saveLongEditText(R.id.RingingPeriod, RcsSettingsData.RINGING_SESSION_PERIOD);
        mHelper.saveLongEditText(R.id.SubscribeExpirePeriod,
                RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD);
        mHelper.saveLongEditText(R.id.IsComposingTimeout, RcsSettingsData.IS_COMPOSING_TIMEOUT);
        mHelper.saveLongEditText(R.id.SessionRefreshExpirePeriod,
                RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD);
        mHelper.saveLongEditText(R.id.CapabilityRefreshTimeout,
                RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT);
        mHelper.saveLongEditText(R.id.CapabilityExpiryTimeout,
                RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT);
        mHelper.saveLongEditText(R.id.CapabilityPollingPeriod,
                RcsSettingsData.CAPABILITY_POLLING_PERIOD);
        mHelper.saveBoolCheckBox(R.id.SipKeepAlive, RcsSettingsData.SIP_KEEP_ALIVE);
        mHelper.saveBoolCheckBox(R.id.PermanentState, RcsSettingsData.PERMANENT_STATE_MODE);
        mHelper.saveBoolCheckBox(R.id.TelUriFormat, RcsSettingsData.TEL_URI_FORMAT);
        mHelper.saveBoolCheckBox(R.id.ImAlwaysOn, RcsSettingsData.IM_CAPABILITY_ALWAYS_ON);
        mHelper.saveBoolCheckBox(R.id.FtAlwaysOn, RcsSettingsData.FT_CAPABILITY_ALWAYS_ON);
        mHelper.saveBoolCheckBox(R.id.FtHttpAlwaysOn, RcsSettingsData.FT_HTTP_CAP_ALWAYS_ON);
        mHelper.saveBoolCheckBox(R.id.InviteOnlyGroupchatSF,
                RcsSettingsData.GROUP_CHAT_INVITE_ONLY_FULL_SF);
        mHelper.saveBoolCheckBox(R.id.ImUseReports, RcsSettingsData.IM_USE_REPORTS);
        mHelper.saveBoolCheckBox(R.id.Gruu, RcsSettingsData.GRUU);
        mHelper.saveBoolCheckBox(R.id.CpuAlwaysOn, RcsSettingsData.CPU_ALWAYS_ON);
        mHelper.saveBoolCheckBox(R.id.SecureMsrpOverWifi, RcsSettingsData.SECURE_MSRP_OVER_WIFI);
        mHelper.saveBoolCheckBox(R.id.SecureRtpOverWifi, RcsSettingsData.SECURE_RTP_OVER_WIFI);
        mHelper.saveBoolCheckBox(R.id.ImeiAsDeviceId, RcsSettingsData.USE_IMEI_AS_DEVICE_ID);
        mHelper.saveBoolCheckBox(R.id.ControlExtensions, RcsSettingsData.CONTROL_EXTENSIONS);
        mHelper.saveBoolCheckBox(R.id.AllowExtensions, RcsSettingsData.ALLOW_EXTENSIONS);
        mHelper.saveIntEditText(R.id.MaxMsrpLengthExtensions,
                RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS);
        mHelper.saveLongEditText(R.id.MessagingCapabilitiesValidity,
                RcsSettingsData.MSG_CAP_VALIDITY_PERIOD);
    }

}
