/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.service;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.EnableRcseSwitch;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Intents;
import com.gsma.services.rcs.RcsService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class to control the service activation.
 * 
 * @author yplo6403
 */
public class RcsServiceControlReceiver extends BroadcastReceiver {

    private final static Logger sLogger = Logger.getLogger(RcsServiceControlReceiver.class
            .getSimpleName());

    private RcsSettings mRcsSettings;

    private Context mContext;

    private static final int INVALID_EXTRA = -1;

    private enum RcsServiceName {
        CAPABILITY("CapabilityService"), CONTACT("ContactService"), CHAT("ChatService"), FILE_TRANSFER(
                "FileTransferService"), FILE_UPLOAD("FileUploadService"), GEOLOC_SHARING(
                "GeolocSharingService"), HISTORY("HistoryService"), IMAGE_SHARING(
                "ImageSharingService"), MULTIMEDIA("MultimediaSessionService"), VIDEO_SHARING(
                "VideoSharingService");

        private String mClassname;

        /* Reverse-lookup map for getting a RcsServiceName from a service classname */
        private static final Map<String, RcsServiceName> sLookup = new HashMap<String, RcsServiceName>();
        static {
            for (RcsServiceName service : RcsServiceName.values())
                sLookup.put(service.mClassname, service);
        }

        RcsServiceName(String classname) {
            mClassname = classname;
        }

        public static RcsServiceName get(String classname) {
            return sLookup.get(classname);
        }

    };

    private static final Set<Pair<Integer, Integer>> sVersionSet001 = new HashSet<Pair<Integer, Integer>>();
    static {
        sVersionSet001.add(new Pair<Integer, Integer>(RcsService.Build.API_VERSION,
                RcsService.Build.API_INCREMENTAL));
    }

    private static final Map<RcsServiceName, Set<Pair<Integer, Integer>>> sServiceCompatibilityMap = new HashMap<RcsServiceName, Set<Pair<Integer, Integer>>>();
    static {
        sServiceCompatibilityMap.put(RcsServiceName.CAPABILITY, sVersionSet001);
        sServiceCompatibilityMap.put(RcsServiceName.CONTACT, sVersionSet001);
        sServiceCompatibilityMap.put(RcsServiceName.CHAT, sVersionSet001);
        sServiceCompatibilityMap.put(RcsServiceName.FILE_TRANSFER, sVersionSet001);
        sServiceCompatibilityMap.put(RcsServiceName.FILE_UPLOAD, sVersionSet001);
        sServiceCompatibilityMap.put(RcsServiceName.GEOLOC_SHARING, sVersionSet001);
        sServiceCompatibilityMap.put(RcsServiceName.HISTORY, sVersionSet001);
        sServiceCompatibilityMap.put(RcsServiceName.IMAGE_SHARING, sVersionSet001);
        sServiceCompatibilityMap.put(RcsServiceName.MULTIMEDIA, sVersionSet001);
        sServiceCompatibilityMap.put(RcsServiceName.VIDEO_SHARING, sVersionSet001);
    }

    private boolean getActivationModeChangeable() {
        EnableRcseSwitch enableRcseSwitch = mRcsSettings.getEnableRcseSwitch();
        switch (enableRcseSwitch) {
            case ALWAYS_SHOW:
                return true;
            case ONLY_SHOW_IN_ROAMING:
                return IsDataRoamingEnabled();
            case NEVER_SHOW:
            default:
                return false;
        }
    }

    private boolean getActivationMode(Context context) {
        return mRcsSettings.isServiceActivated();
    }

    private boolean IsDataRoamingEnabled() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null) {
            return false;
        }
        return cm.getActiveNetworkInfo().isRoaming();
    }

    private void setActivationMode(Context context, boolean active) {
        if (!getActivationModeChangeable()) {
            if (sLogger.isActivated()) {
                sLogger.error("Cannot set activation mode: permission denied");
            }
            return;
        }
        if (mRcsSettings.isServiceActivated() == active) {
            if (sLogger.isActivated()) {
                sLogger.warn("Activation mode already set to ".concat(String.valueOf(active)));
            }
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("setActivationMode: ".concat(String.valueOf(active)));
        }
        mRcsSettings.setServiceActivationState(active);
        if (active) {
            LauncherUtils.launchRcsService(mContext, false, true, mRcsSettings);
        } else {
            LauncherUtils.stopRcsService(mContext);
        }
    }

    private boolean isCompatible(String servicename, String codename, int version, int increment) {
        if (TextUtils.isEmpty(codename) || version == INVALID_EXTRA || increment == INVALID_EXTRA) {
            return false;
        }
        if (!RcsService.Build.API_CODENAME.equals(codename)) {
            return false;
        }
        RcsServiceName rcsService = RcsServiceName.get(servicename);
        if (rcsService == null) {
            return false;
        }
        Set<Pair<Integer, Integer>> authorizedVersions = sServiceCompatibilityMap.get(rcsService);
        if (authorizedVersions == null) {
            return false;
        }
        Pair<Integer, Integer> clientVersion = new Pair<Integer, Integer>(version, increment);
        return (authorizedVersions.contains(clientVersion));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LocalContentResolver localContentResolver = new LocalContentResolver(context);
        mRcsSettings = RcsSettings.createInstance(localContentResolver);
        mContext = context;

        if (Intents.Service.ACTION_GET_ACTIVATION_MODE_CHANGEABLE.equals(intent.getAction())) {
            Bundle results = getResultExtras(true);
            if (results == null) {
                return;
            }
            results.putBoolean(Intents.Service.EXTRA_GET_ACTIVATION_MODE_CHANGEABLE,
                    getActivationModeChangeable());
            setResultExtras(results);
        } else if (Intents.Service.ACTION_GET_ACTIVATION_MODE.equals(intent.getAction())) {
            Bundle results = getResultExtras(true);
            if (results == null) {
                return;
            }
            results.putBoolean(Intents.Service.EXTRA_GET_ACTIVATION_MODE,
                    getActivationMode(context));
            setResultExtras(results);
        } else if (Intents.Service.ACTION_SET_ACTIVATION_MODE.equals(intent.getAction())) {
            boolean active = intent
                    .getBooleanExtra(Intents.Service.EXTRA_SET_ACTIVATION_MODE, true);
            setActivationMode(context, active);
        } else if (Intents.Service.ACTION_GET_COMPATIBLITY.equals(intent.getAction())) {
            Bundle results = getResultExtras(true);
            if (results == null) {
                return;
            }
            String servicename = intent
                    .getStringExtra(Intents.Service.EXTRA_GET_COMPATIBLITY_SERVICE);
            String codename = intent
                    .getStringExtra(Intents.Service.EXTRA_GET_COMPATIBLITY_CODENAME);
            int version = intent.getIntExtra(Intents.Service.EXTRA_GET_COMPATIBLITY_VERSION,
                    INVALID_EXTRA);
            int increment = intent.getIntExtra(Intents.Service.EXTRA_GET_COMPATIBLITY_INCREMENT,
                    INVALID_EXTRA);

            results.putBoolean(Intents.Service.EXTRA_GET_COMPATIBLITY_RESPONSE,
                    isCompatible(servicename, codename, version, increment));
            setResultExtras(results);
        }
    }

}
