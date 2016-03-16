/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.extension;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.registration.RegistrationManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.capability.CapabilityService;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to update the supported extensions
 * 
 * @author yplo6403
 */
public class SupportedExtensionUpdater implements Runnable {

    private final static Logger sLogger = Logger.getLogger(SupportedExtensionUpdater.class
            .getSimpleName());

    private final Context mCtx;

    private final ServiceExtensionManager mExtensionManager;

    private final ImsModule mImsModule;

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param ctx
     * @param imsModule
     * @param rcsSettings
     * @param extensionManager
     */
    public SupportedExtensionUpdater(Context ctx, ImsModule imsModule, RcsSettings rcsSettings,
            ServiceExtensionManager extensionManager) {
        mCtx = ctx;
        mImsModule = imsModule;
        mRcsSettings = rcsSettings;
        mExtensionManager = extensionManager;
    }

    @Override
    public void run() {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Update supported extensions");
        }
        try {
            Set<String> supportedExts = new HashSet<String>();
            Set<String> oldSupportedExts = mRcsSettings.getSupportedRcsExtensions();
            /* Intent query on current installed activities */
            PackageManager pm = mCtx.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo appInfo : apps) {
                Bundle appMeta = appInfo.metaData;
                if (appMeta == null) {
                    continue;
                }
                String exts = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
                if (TextUtils.isEmpty(exts)) {
                    continue;
                }
                Set<String> extensions = ServiceExtensionManager.getExtensions(exts);
                for (String extension : extensions) {
                    if (mExtensionManager.isExtensionAuthorized(extension)) {
                        supportedExts.add(extension);
                    }
                }
            }
            if (oldSupportedExts.equals(supportedExts)) {
                /* No change in supported extensions: exit */
                return;
            }
            if (sLogger.isActivated()) {
                StringBuilder sb = new StringBuilder("Supported extensions changed!");
                Set<String> newExtensions = new HashSet<String>(supportedExts);
                newExtensions.removeAll(oldSupportedExts);
                if (!newExtensions.isEmpty()) {
                    sb.append(" new=".concat(Arrays.toString(newExtensions.toArray())));
                }
                Set<String> removedExtensions = new HashSet<String>(oldSupportedExts);
                removedExtensions.removeAll(supportedExts);
                if (!removedExtensions.isEmpty()) {
                    sb.append(" removed=".concat(Arrays.toString(removedExtensions.toArray())));
                }
                sLogger.debug(sb.toString());
            }
            /* Update supported extensions in database */
            mExtensionManager.saveSupportedExtensions(supportedExts);
            if (!mImsModule.getCore().isStarted()) {
                /* Stack is not started, don't process this event */
                return;
            }
            RegistrationManager registrationManager = mImsModule.getSipManager()
                    .getNetworkInterface().getRegistrationManager();
            if (registrationManager.isRegistered()) {
                registrationManager.restart();
            }
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to update supported extensions!", e);
        }
    }

}
