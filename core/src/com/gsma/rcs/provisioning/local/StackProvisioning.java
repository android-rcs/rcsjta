/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

import static com.gsma.rcs.provisioning.local.Provisioning.saveCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveIntegerEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveLongEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveStringEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setIntegerEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setLongEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setStringEditTextParam;

import com.gsma.rcs.R;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.provider.settings.RcsSettingsData.ConfigurationMode;
import com.gsma.rcs.provider.settings.RcsSettingsData.EnableRcseSwitch;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.provider.settings.RcsSettingsData.NetworkAccessType;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Stack parameters provisioning File
 * 
 * @author jexa7410
 */
public class StackProvisioning extends Activity {
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
            RcsSettingsData.FileTransferProtocol.HTTP.name(),
            RcsSettingsData.FileTransferProtocol.MSRP.name()
    };

    private RcsSettings mRcsSettings;

    private boolean isInFront;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Set layout
        setContentView(R.layout.rcs_provisioning_stack);

        // Set buttons callback
        Button btn = (Button) findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        mConfigModes = getResources().getStringArray(R.array.provisioning_config_mode);
        mNetworkAccesses = getResources().getStringArray(R.array.provisioning_network_access);
        mEnableRcseSwitch = getResources().getStringArray(R.array.provisioning_enable_rcs_switch);
        mRcsSettings = RcsSettings.getInstance(new LocalContentResolver(this));
        updateView(bundle);
        isInFront = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        saveInstanceState(bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isInFront) {
            isInFront = true;
            // Update UI (from DB)
            updateView(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInFront = false;
    }

    /**
     * Save parameters either in bundle or in RCS settings
     */
    private void saveInstanceState(Bundle bundle) {
        final ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        Spinner spinner = (Spinner) findViewById(R.id.Autoconfig);
        switch (spinner.getSelectedItemPosition()) {
            case 0:
                if (bundle != null) {
                    bundle.putInt(RcsSettingsData.CONFIG_MODE, ConfigurationMode.MANUAL.toInt());
                } else {
                    mRcsSettings.setConfigurationMode(ConfigurationMode.MANUAL);
                }
                break;
            case 1:
            default:
                if (bundle != null) {
                    bundle.putInt(RcsSettingsData.CONFIG_MODE, ConfigurationMode.AUTO.toInt());
                } else {
                    mRcsSettings.setConfigurationMode(ConfigurationMode.AUTO);
                }
                break;
        }

        spinner = (Spinner) findViewById(R.id.SipDefaultProtocolForMobile);
        if (bundle != null) {
            bundle.putString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
                    (String) spinner.getSelectedItem());
        } else {
            mRcsSettings.writeString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
                    (String) spinner.getSelectedItem());
        }

        spinner = (Spinner) findViewById(R.id.EnableRcsSwitch);
        switch (spinner.getSelectedItemPosition()) {
            case 1:
                if (bundle != null) {
                    bundle.putInt(RcsSettingsData.ENABLE_RCS_SWITCH,
                            EnableRcseSwitch.ALWAYS_SHOW.toInt());
                } else {
                    mRcsSettings.setEnableRcseSwitch(EnableRcseSwitch.ALWAYS_SHOW);
                }
                break;
            case 0:
                if (bundle != null) {
                    bundle.putInt(RcsSettingsData.ENABLE_RCS_SWITCH,
                            EnableRcseSwitch.ONLY_SHOW_IN_ROAMING.toInt());
                } else {
                    mRcsSettings.setEnableRcseSwitch(EnableRcseSwitch.ONLY_SHOW_IN_ROAMING);
                }
                break;
            default:
                if (bundle != null) {
                    bundle.putInt(RcsSettingsData.ENABLE_RCS_SWITCH,
                            EnableRcseSwitch.NEVER_SHOW.toInt());
                } else {
                    mRcsSettings.setEnableRcseSwitch(EnableRcseSwitch.NEVER_SHOW);
                }
        }

        spinner = (Spinner) findViewById(R.id.SipDefaultProtocolForWifi);
        if (bundle != null) {
            bundle.putString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
                    (String) spinner.getSelectedItem());
        } else {
            mRcsSettings.writeString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
                    (String) spinner.getSelectedItem());
        }

        saveCheckBoxParam(R.id.TcpFallback, RcsSettingsData.TCP_FALLBACK, helper);

        spinner = (Spinner) findViewById(R.id.TlsCertificateRoot);
        if (spinner.getSelectedItemPosition() == 0) {
            if (bundle != null) {
                bundle.putString(RcsSettingsData.TLS_CERTIFICATE_ROOT, null);
            } else {
                mRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_ROOT, null);
            }
        } else {
            String path = CERTIFICATE_FOLDER_PATH + File.separator
                    + spinner.getSelectedItem();
            if (bundle != null) {
                bundle.putString(RcsSettingsData.TLS_CERTIFICATE_ROOT, path);
            } else {
                mRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_ROOT, path);
            }
        }

        spinner = (Spinner) findViewById(R.id.TlsCertificateIntermediate);
        if (spinner.getSelectedItemPosition() == 0) {
            if (bundle != null) {
                bundle.putString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, null);
            } else {
                mRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, null);
            }
        } else {
            String path = CERTIFICATE_FOLDER_PATH + File.separator
                    + spinner.getSelectedItem();
            if (bundle != null) {
                bundle.putString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, path);
            } else {
                mRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, path);
            }
        }

        spinner = (Spinner) findViewById(R.id.NetworkAccess);
        switch (spinner.getSelectedItemPosition()) {
            case 1:
                if (bundle != null) {
                    bundle.putInt(RcsSettingsData.NETWORK_ACCESS, NetworkAccessType.MOBILE.toInt());
                } else {
                    mRcsSettings.setNetworkAccess(NetworkAccessType.MOBILE);
                }
                break;
            case 2:
                if (bundle != null) {
                    bundle.putInt(RcsSettingsData.NETWORK_ACCESS, NetworkAccessType.WIFI.toInt());
                } else {
                    mRcsSettings.setNetworkAccess(NetworkAccessType.WIFI);
                }
                break;
            default:
                if (bundle != null) {
                    bundle.putInt(RcsSettingsData.NETWORK_ACCESS, NetworkAccessType.ANY.toInt());
                } else {
                    mRcsSettings.setNetworkAccess(NetworkAccessType.ANY);
                }
        }

        spinner = (Spinner) findViewById(R.id.FtProtocol);
        if (bundle != null) {
            bundle.putString(RcsSettingsData.FT_PROTOCOL, (String) spinner.getSelectedItem());
        } else {
            FileTransferProtocol protocol = FileTransferProtocol.valueOf((String) spinner
                    .getSelectedItem());
            mRcsSettings.setFtProtocol(protocol);
        }

        saveStringEditTextParam(R.id.SecondaryProvisioningAddress,
                RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS, helper);
        saveCheckBoxParam(R.id.SecondaryProvisioningAddressOnly,
                RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY, helper);
        saveLongEditTextParam(R.id.ImsServicePollingPeriod,
                RcsSettingsData.IMS_SERVICE_POLLING_PERIOD, helper);
        saveIntegerEditTextParam(R.id.SipListeningPort, RcsSettingsData.SIP_DEFAULT_PORT, helper);
        saveLongEditTextParam(R.id.SipTimerT1, RcsSettingsData.SIP_TIMER_T1, helper);
        saveLongEditTextParam(R.id.SipTimerT2, RcsSettingsData.SIP_TIMER_T2, helper);
        saveLongEditTextParam(R.id.SipTimerT4, RcsSettingsData.SIP_TIMER_T4, helper);
        saveLongEditTextParam(R.id.SipTransactionTimeout, RcsSettingsData.SIP_TRANSACTION_TIMEOUT,
                helper);
        saveLongEditTextParam(R.id.SipKeepAlivePeriod, RcsSettingsData.SIP_KEEP_ALIVE_PERIOD,
                helper);
        saveIntegerEditTextParam(R.id.DefaultMsrpPort, RcsSettingsData.MSRP_DEFAULT_PORT, helper);
        saveIntegerEditTextParam(R.id.DefaultRtpPort, RcsSettingsData.RTP_DEFAULT_PORT, helper);
        saveLongEditTextParam(R.id.MsrpTransactionTimeout,
                RcsSettingsData.MSRP_TRANSACTION_TIMEOUT, helper);
        saveLongEditTextParam(R.id.RegisterExpirePeriod, RcsSettingsData.REGISTER_EXPIRE_PERIOD,
                helper);
        saveLongEditTextParam(R.id.RegisterRetryBaseTime, RcsSettingsData.REGISTER_RETRY_BASE_TIME,
                helper);
        saveLongEditTextParam(R.id.RegisterRetryMaxTime, RcsSettingsData.REGISTER_RETRY_MAX_TIME,
                helper);
        saveLongEditTextParam(R.id.PublishExpirePeriod, RcsSettingsData.PUBLISH_EXPIRE_PERIOD,
                helper);
        saveLongEditTextParam(R.id.RevokeTimeout, RcsSettingsData.REVOKE_TIMEOUT, helper);
        saveLongEditTextParam(R.id.RingingPeriod, RcsSettingsData.RINGING_SESSION_PERIOD, helper);
        saveLongEditTextParam(R.id.SubscribeExpirePeriod, RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD,
                helper);
        saveLongEditTextParam(R.id.IsComposingTimeout, RcsSettingsData.IS_COMPOSING_TIMEOUT, helper);
        saveLongEditTextParam(R.id.SessionRefreshExpirePeriod,
                RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD, helper);
        saveLongEditTextParam(R.id.CapabilityRefreshTimeout,
                RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT, helper);
        saveLongEditTextParam(R.id.CapabilityExpiryTimeout,
                RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT, helper);
        saveLongEditTextParam(R.id.CapabilityPollingPeriod,
                RcsSettingsData.CAPABILITY_POLLING_PERIOD, helper);
        saveCheckBoxParam(R.id.SipKeepAlive, RcsSettingsData.SIP_KEEP_ALIVE, helper);
        saveCheckBoxParam(R.id.PermanentState, RcsSettingsData.PERMANENT_STATE_MODE, helper);
        saveCheckBoxParam(R.id.TelUriFormat, RcsSettingsData.TEL_URI_FORMAT, helper);
        saveCheckBoxParam(R.id.ImAlwaysOn, RcsSettingsData.IM_CAPABILITY_ALWAYS_ON, helper);
        saveCheckBoxParam(R.id.FtAlwaysOn, RcsSettingsData.FT_CAPABILITY_ALWAYS_ON, helper);
        saveCheckBoxParam(R.id.FtHttpAlwaysOn, RcsSettingsData.FT_HTTP_CAP_ALWAYS_ON, helper);
        saveCheckBoxParam(R.id.InviteOnlyGroupchatSF,
                RcsSettingsData.GROUP_CHAT_INVITE_ONLY_FULL_SF, helper);
        saveCheckBoxParam(R.id.ImUseReports, RcsSettingsData.IM_USE_REPORTS, helper);
        saveCheckBoxParam(R.id.Gruu, RcsSettingsData.GRUU, helper);
        saveCheckBoxParam(R.id.CpuAlwaysOn, RcsSettingsData.CPU_ALWAYS_ON, helper);
        saveCheckBoxParam(R.id.SecureMsrpOverWifi, RcsSettingsData.SECURE_MSRP_OVER_WIFI, helper);
        saveCheckBoxParam(R.id.SecureRtpOverWifi, RcsSettingsData.SECURE_RTP_OVER_WIFI, helper);
        saveCheckBoxParam(R.id.ImeiAsDeviceId, RcsSettingsData.USE_IMEI_AS_DEVICE_ID, helper);
        saveCheckBoxParam(R.id.ControlExtensions, RcsSettingsData.CONTROL_EXTENSIONS, helper);
        saveCheckBoxParam(R.id.AllowExtensions, RcsSettingsData.ALLOW_EXTENSIONS, helper);
        saveIntegerEditTextParam(R.id.MaxMsrpLengthExtensions,
                RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS, helper);
        saveLongEditTextParam(R.id.MessagingCapabilitiesValidity,
                RcsSettingsData.MSG_CAP_VALIDITY_PERIOD, helper);
    }

    /**
     * Update UI (upon creation, rotation, tab switch...)
     * 
     * @param bundle The bundle to save settings
     */
    private void updateView(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);
        // Display stack parameters
        Spinner spinner = (Spinner) findViewById(R.id.Autoconfig);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, mConfigModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        ConfigurationMode mode;
        if (bundle != null && bundle.containsKey(RcsSettingsData.CONFIG_MODE)) {
            mode = ConfigurationMode.valueOf(bundle.getInt(RcsSettingsData.CONFIG_MODE));
        } else {
            mode = mRcsSettings.getConfigurationMode();
        }
        spinner.setSelection(ConfigurationMode.AUTO.equals(mode) ? 1 : 0);

        spinner = (Spinner) findViewById(R.id.client_vendor);
        final String[] vendorArray = new String[] {
            Build.MANUFACTURER
        };
        ArrayAdapter<CharSequence> adapterVendor = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item, vendorArray);
        adapterVendor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterVendor);
        spinner.setSelection(0);

        spinner = (Spinner) findViewById(R.id.EnableRcsSwitch);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                mEnableRcseSwitch);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        EnableRcseSwitch rcsSwitch;
        if (bundle != null && bundle.containsKey(RcsSettingsData.ENABLE_RCS_SWITCH)) {
            rcsSwitch = EnableRcseSwitch.valueOf(bundle.getInt(RcsSettingsData.ENABLE_RCS_SWITCH));
        } else {
            rcsSwitch = mRcsSettings.getEnableRcseSwitch();
        }
        switch (rcsSwitch) {
            case ALWAYS_SHOW:
            case ONLY_SHOW_IN_ROAMING:
                spinner.setSelection(rcsSwitch.toInt());
                break;
            default:
                spinner.setSelection(2);
        }

        setStringEditTextParam(R.id.SecondaryProvisioningAddress,
                RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS, helper);
        setCheckBoxParam(R.id.SecondaryProvisioningAddressOnly,
                RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY, helper);

        spinner = (Spinner) findViewById(R.id.NetworkAccess);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                mNetworkAccesses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        NetworkAccessType access;
        if (bundle != null && bundle.containsKey(RcsSettingsData.NETWORK_ACCESS)) {
            access = NetworkAccessType.valueOf(bundle.getInt(RcsSettingsData.NETWORK_ACCESS));
        } else {
            access = mRcsSettings.getNetworkAccess();
        }
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

        spinner = (Spinner) findViewById(R.id.SipDefaultProtocolForMobile);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SIP_PROTOCOL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String sipMobile;
        if (bundle != null && bundle.containsKey(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE)) {
            sipMobile = bundle.getString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE);
        } else {
            sipMobile = mRcsSettings.getSipDefaultProtocolForMobile();
        }
        if (SIP_PROTOCOL[0].equalsIgnoreCase(sipMobile)) {
            spinner.setSelection(0);
        } else if (SIP_PROTOCOL[1].equalsIgnoreCase(sipMobile)) {
            spinner.setSelection(1);
        } else {
            spinner.setSelection(2);
        }

        spinner = (Spinner) findViewById(R.id.SipDefaultProtocolForWifi);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SIP_PROTOCOL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String sipWifi;
        if (bundle != null && bundle.containsKey(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI)) {
            sipWifi = bundle.getString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI);
        } else {
            sipWifi = mRcsSettings.getSipDefaultProtocolForWifi();
        }
        if (SIP_PROTOCOL[0].equalsIgnoreCase(sipWifi)) {
            spinner.setSelection(0);
        } else if (SIP_PROTOCOL[1].equalsIgnoreCase(sipWifi)) {
            spinner.setSelection(1);
        } else {
            spinner.setSelection(2);
        }

        String[] certificates = loadCertificatesList();
        spinner = (Spinner) findViewById(R.id.TlsCertificateRoot);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, certificates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        boolean found = false;
        String certRoot;
        if (bundle != null && bundle.containsKey(RcsSettingsData.TLS_CERTIFICATE_ROOT)) {
            certRoot = bundle.getString(RcsSettingsData.TLS_CERTIFICATE_ROOT);
        } else {
            certRoot = mRcsSettings.getTlsCertificateRoot();
        }
        for (int i = 0; i < certificates.length; i++) {
            if (certRoot != null && certRoot.contains(certificates[i])) {
                spinner.setSelection(i);
                found = true;
            }
        }
        if (!found) {
            spinner.setSelection(0);
            mRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_ROOT, "");
        }

        spinner = (Spinner) findViewById(R.id.TlsCertificateIntermediate);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, certificates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        found = false;
        String certInt;
        if (bundle != null && bundle.containsKey(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE)) {
            certInt = bundle.getString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE);
        } else {
            certInt = mRcsSettings.getTlsCertificateIntermediate();
        }
        for (int i = 0; i < certificates.length; i++) {
            if (certInt != null && certInt.contains(certificates[i])) {
                spinner.setSelection(i);
                found = true;
            }
        }
        if (!found) {
            spinner.setSelection(0);
            mRcsSettings.writeString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE, "");
        }

        spinner = (Spinner) findViewById(R.id.FtProtocol);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, FT_PROTOCOL);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        FileTransferProtocol ftProtocol;
        if (bundle != null && bundle.containsKey(RcsSettingsData.FT_PROTOCOL)) {
            ftProtocol = FileTransferProtocol
                    .valueOf(bundle.getString(RcsSettingsData.FT_PROTOCOL));
        } else {
            ftProtocol = mRcsSettings.getFtProtocol();
        }
        if (FileTransferProtocol.HTTP.equals(ftProtocol)) {
            spinner.setSelection(0);
        } else {
            spinner.setSelection(1);
        }

        setLongEditTextParam(R.id.ImsServicePollingPeriod,
                RcsSettingsData.IMS_SERVICE_POLLING_PERIOD, helper);
        setIntegerEditTextParam(R.id.SipListeningPort, RcsSettingsData.SIP_DEFAULT_PORT, helper);
        setLongEditTextParam(R.id.SipTimerT1, RcsSettingsData.SIP_TIMER_T1, helper);
        setLongEditTextParam(R.id.SipTimerT2, RcsSettingsData.SIP_TIMER_T2, helper);
        setLongEditTextParam(R.id.SipTimerT4, RcsSettingsData.SIP_TIMER_T4, helper);
        setLongEditTextParam(R.id.SipTransactionTimeout, RcsSettingsData.SIP_TRANSACTION_TIMEOUT,
                helper);
        setLongEditTextParam(R.id.SipKeepAlivePeriod, RcsSettingsData.SIP_KEEP_ALIVE_PERIOD, helper);
        setIntegerEditTextParam(R.id.DefaultMsrpPort, RcsSettingsData.MSRP_DEFAULT_PORT, helper);
        setIntegerEditTextParam(R.id.DefaultRtpPort, RcsSettingsData.RTP_DEFAULT_PORT, helper);
        setLongEditTextParam(R.id.MsrpTransactionTimeout, RcsSettingsData.MSRP_TRANSACTION_TIMEOUT,
                helper);
        setLongEditTextParam(R.id.RegisterExpirePeriod, RcsSettingsData.REGISTER_EXPIRE_PERIOD,
                helper);
        setLongEditTextParam(R.id.RegisterRetryBaseTime, RcsSettingsData.REGISTER_RETRY_BASE_TIME,
                helper);
        setLongEditTextParam(R.id.RegisterRetryMaxTime, RcsSettingsData.REGISTER_RETRY_MAX_TIME,
                helper);
        setLongEditTextParam(R.id.PublishExpirePeriod, RcsSettingsData.PUBLISH_EXPIRE_PERIOD,
                helper);
        setLongEditTextParam(R.id.RevokeTimeout, RcsSettingsData.REVOKE_TIMEOUT, helper);
        setLongEditTextParam(R.id.RingingPeriod, RcsSettingsData.RINGING_SESSION_PERIOD, helper);
        setLongEditTextParam(R.id.SubscribeExpirePeriod, RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD,
                helper);
        setLongEditTextParam(R.id.IsComposingTimeout, RcsSettingsData.IS_COMPOSING_TIMEOUT, helper);
        setLongEditTextParam(R.id.SessionRefreshExpirePeriod,
                RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD, helper);
        setLongEditTextParam(R.id.CapabilityRefreshTimeout,
                RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT, helper);
        setLongEditTextParam(R.id.CapabilityExpiryTimeout,
                RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT, helper);
        setLongEditTextParam(R.id.CapabilityPollingPeriod,
                RcsSettingsData.CAPABILITY_POLLING_PERIOD, helper);
        setCheckBoxParam(R.id.TcpFallback, RcsSettingsData.TCP_FALLBACK, helper);
        setCheckBoxParam(R.id.SipKeepAlive, RcsSettingsData.SIP_KEEP_ALIVE, helper);
        setCheckBoxParam(R.id.PermanentState, RcsSettingsData.PERMANENT_STATE_MODE, helper);
        setCheckBoxParam(R.id.TelUriFormat, RcsSettingsData.TEL_URI_FORMAT, helper);
        setCheckBoxParam(R.id.ImAlwaysOn, RcsSettingsData.IM_CAPABILITY_ALWAYS_ON, helper);
        setCheckBoxParam(R.id.FtAlwaysOn, RcsSettingsData.FT_CAPABILITY_ALWAYS_ON, helper);
        setCheckBoxParam(R.id.FtHttpAlwaysOn, RcsSettingsData.FT_HTTP_CAP_ALWAYS_ON, helper);
        setCheckBoxParam(R.id.InviteOnlyGroupchatSF,
                RcsSettingsData.GROUP_CHAT_INVITE_ONLY_FULL_SF, helper);
        setCheckBoxParam(R.id.ImUseReports, RcsSettingsData.IM_USE_REPORTS, helper);
        setCheckBoxParam(R.id.Gruu, RcsSettingsData.GRUU, helper);
        setCheckBoxParam(R.id.CpuAlwaysOn, RcsSettingsData.CPU_ALWAYS_ON, helper);
        setCheckBoxParam(R.id.SecureMsrpOverWifi, RcsSettingsData.SECURE_MSRP_OVER_WIFI, helper);
        setCheckBoxParam(R.id.SecureRtpOverWifi, RcsSettingsData.SECURE_RTP_OVER_WIFI, helper);
        setCheckBoxParam(R.id.ImeiAsDeviceId, RcsSettingsData.USE_IMEI_AS_DEVICE_ID, helper);
        setCheckBoxParam(R.id.ControlExtensions, RcsSettingsData.CONTROL_EXTENSIONS, helper);
        setCheckBoxParam(R.id.AllowExtensions, RcsSettingsData.ALLOW_EXTENSIONS, helper);
        setIntegerEditTextParam(R.id.MaxMsrpLengthExtensions,
                RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS, helper);
        setLongEditTextParam(R.id.MessagingCapabilitiesValidity,
                RcsSettingsData.MSG_CAP_VALIDITY_PERIOD, helper);
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
            // Save parameters
            saveInstanceState(null);
            Toast.makeText(StackProvisioning.this, getString(R.string.label_reboot_service),
                    Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Load a list of certificates from the SDCARD
     * 
     * @return List of certificates
     */
    private String[] loadCertificatesList() {
        String[] files = null;
        File folder = new File(CERTIFICATE_FOLDER_PATH);
        try {
            //noinspection ResultOfMethodCallIgnored
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

}
