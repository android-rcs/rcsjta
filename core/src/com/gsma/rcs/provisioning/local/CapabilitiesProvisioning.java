/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2016 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provisioning.local;

import com.gsma.rcs.R;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.utils.logger.Logger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Logger provisioning fragment
 * 
 * @author jexa7410
 */
public class CapabilitiesProvisioning extends Fragment implements IProvisioningFragment {

    private static final Logger sLogger = Logger
            .getLogger(CapabilitiesProvisioning.class.getName());

    private static RcsSettings sRcsSettings;
    private ProvisioningHelper mHelper;

    public static CapabilitiesProvisioning newInstance(RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.debug("new instance");
        }
        CapabilitiesProvisioning f = new CapabilitiesProvisioning();
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
        View rootView = inflater.inflate(R.layout.provisioning_capabilities, container, false);
        mHelper = new ProvisioningHelper(rootView, sRcsSettings);
        displayRcsSettings();
        return rootView;
    }

    @Override
    public void displayRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("displayRcsSettings");
        }
        mHelper.setBoolCheckBox(R.id.image_sharing, RcsSettingsData.CAPABILITY_IMAGE_SHARING);
        mHelper.setBoolCheckBox(R.id.video_sharing, RcsSettingsData.CAPABILITY_VIDEO_SHARING);
        mHelper.setBoolCheckBox(R.id.file_transfer_msrp, RcsSettingsData.CAPABILITY_FILE_TRANSFER);
        mHelper.setBoolCheckBox(R.id.file_transfer_http,
                RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP);
        mHelper.setBoolCheckBox(R.id.im, RcsSettingsData.CAPABILITY_IM_SESSION);
        mHelper.setBoolCheckBox(R.id.im_group, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION);
        mHelper.setBoolCheckBox(R.id.ipvoicecall, RcsSettingsData.CAPABILITY_IP_VOICE_CALL);
        mHelper.setBoolCheckBox(R.id.ipvideocall, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL);
        mHelper.setBoolCheckBox(R.id.cs_video, RcsSettingsData.CAPABILITY_CS_VIDEO);
        mHelper.setBoolCheckBox(R.id.presence_discovery,
                RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY);
        mHelper.setBoolCheckBox(R.id.social_presence, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE);
        mHelper.setBoolCheckBox(R.id.geolocation_push, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH);
        mHelper.setBoolCheckBox(R.id.file_transfer_thumbnail,
                RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL);
        mHelper.setBoolCheckBox(R.id.file_transfer_sf, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF);
        mHelper.setBoolCheckBox(R.id.group_chat_sf, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF);
        mHelper.setBoolCheckBox(R.id.sip_automata, RcsSettingsData.CAPABILITY_SIP_AUTOMATA);
        mHelper.setBoolCheckBox(R.id.call_composer, RcsSettingsData.CAPABILITY_CALL_COMPOSER);
        mHelper.setBoolCheckBox(R.id.shared_map, RcsSettingsData.CAPABILITY_SHARED_MAP);
        mHelper.setBoolCheckBox(R.id.shared_sketch, RcsSettingsData.CAPABILITY_SHARED_SKETCH);
        mHelper.setBoolCheckBox(R.id.post_call, RcsSettingsData.CAPABILITY_POST_CALL);
    }

    @Override
    public void persistRcsSettings() {
        if (sLogger.isActivated()) {
            sLogger.debug("persistRcsSettings");
        }
        mHelper.saveBoolCheckBox(R.id.image_sharing, RcsSettingsData.CAPABILITY_IMAGE_SHARING);
        mHelper.saveBoolCheckBox(R.id.video_sharing, RcsSettingsData.CAPABILITY_VIDEO_SHARING);
        mHelper.saveBoolCheckBox(R.id.file_transfer_msrp, RcsSettingsData.CAPABILITY_FILE_TRANSFER);
        mHelper.saveBoolCheckBox(R.id.file_transfer_http,
                RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP);
        mHelper.saveBoolCheckBox(R.id.im, RcsSettingsData.CAPABILITY_IM_SESSION);
        mHelper.saveBoolCheckBox(R.id.im_group, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION);
        mHelper.saveBoolCheckBox(R.id.ipvoicecall, RcsSettingsData.CAPABILITY_IP_VOICE_CALL);
        mHelper.saveBoolCheckBox(R.id.ipvideocall, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL);
        mHelper.saveBoolCheckBox(R.id.cs_video, RcsSettingsData.CAPABILITY_CS_VIDEO);
        mHelper.saveBoolCheckBox(R.id.presence_discovery,
                RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY);
        mHelper.saveBoolCheckBox(R.id.social_presence, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE);
        mHelper.saveBoolCheckBox(R.id.geolocation_push, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH);
        mHelper.saveBoolCheckBox(R.id.file_transfer_thumbnail,
                RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL);
        mHelper.saveBoolCheckBox(R.id.file_transfer_sf, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF);
        mHelper.saveBoolCheckBox(R.id.group_chat_sf, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF);
        mHelper.saveBoolCheckBox(R.id.sip_automata, RcsSettingsData.CAPABILITY_SIP_AUTOMATA);
        mHelper.saveBoolCheckBox(R.id.call_composer, RcsSettingsData.CAPABILITY_CALL_COMPOSER);
        mHelper.saveBoolCheckBox(R.id.shared_map, RcsSettingsData.CAPABILITY_SHARED_MAP);
        mHelper.saveBoolCheckBox(R.id.shared_sketch, RcsSettingsData.CAPABILITY_SHARED_SKETCH);
        mHelper.saveBoolCheckBox(R.id.post_call, RcsSettingsData.CAPABILITY_POST_CALL);
    }

}
