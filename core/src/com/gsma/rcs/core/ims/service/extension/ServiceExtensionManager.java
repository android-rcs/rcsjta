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

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.service.capability.ExternalCapabilityMonitoring;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service extension manager which adds supported extension after having verified some authorization
 * rules.
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ServiceExtensionManager {

    private static final String EXTENSION_SEPARATOR = ";";

    private final static Logger sLogger = Logger.getLogger(ServiceExtensionManager.class
            .getSimpleName());

    private final RcsSettings mRcsSettings;

    private final Context mCtx;

    private final Core mCore;

    private ExternalCapabilityMonitoring mExternalCapabilityMonitoring;

    private ExecutorService mUpdateExecutor;

    private final SupportedExtensionUpdater mSupportedExtensionUpdater;

    /**
     * Monitor the application package changes to update RCS supported extensions
     * 
     * @param imsModule The IMS module
     * @param ctx The app context
     * @param core The core instance
     * @param rcsSettings The RCS settigns accessor
     */
    public ServiceExtensionManager(ImsModule imsModule, Context ctx, Core core,
            RcsSettings rcsSettings) {
        mCtx = ctx;
        mCore = core;
        mRcsSettings = rcsSettings;
        mSupportedExtensionUpdater = new SupportedExtensionUpdater(mCtx, imsModule, mRcsSettings,
                this);
    }

    /**
     * Starts extension manager
     */
    public void start() {
        mUpdateExecutor = Executors.newSingleThreadExecutor();
        updateSupportedExtensions();
        if (mExternalCapabilityMonitoring == null) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addDataScheme("package");
            mExternalCapabilityMonitoring = new ExternalCapabilityMonitoring(mCore, this);
            mCtx.registerReceiver(mExternalCapabilityMonitoring, filter);
        }
    }

    /**
     * Stops extension manager
     */
    public void stop() {
        if (mExternalCapabilityMonitoring != null) {
            mCtx.unregisterReceiver(mExternalCapabilityMonitoring);
            mExternalCapabilityMonitoring = null;
        }
        mUpdateExecutor.shutdownNow();
    }

    /**
     * Save supported extensions in database
     * 
     * @param supportedExts List of supported extensions
     */
    public void saveSupportedExtensions(Set<String> supportedExts) {
        /* Update supported extensions in database */
        mRcsSettings.setSupportedRcsExtensions(supportedExts);
    }

    private void updateSupportedExtensions() {
        mUpdateExecutor.execute(mSupportedExtensionUpdater);
    }

    /**
     * Is extension authorized
     * 
     * @param ext Extension ID
     * @return Boolean
     */
    public boolean isExtensionAuthorized(String ext) {
        if (!mRcsSettings.isExtensionsAllowed()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Extensions are not allowed");
            }
            return false;
        }
        if (mRcsSettings.isExtensionAuthorized(ext)) {
            if (sLogger.isActivated()) {
                sLogger.debug("No control on extension ".concat(ext));
            }
            return true;
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("Extension " + ext + " is not allowed");
            }
            return false;
        }
    }

    /**
     * Remove supported extensions
     */
    public void removeSupportedExtensions() {
        updateSupportedExtensions();
    }

    /**
     * Add supported extensions
     */
    public void addNewSupportedExtensions() {
        updateSupportedExtensions();
    }

    /**
     * Extract set of extensions from String
     * 
     * @param extensions String where extensions are concatenated with a ";" separator
     * @return the set of extensions
     */
    public static Set<String> getExtensions(String extensions) {
        Set<String> result = new HashSet<>();
        if (TextUtils.isEmpty(extensions)) {
            return result;
        }
        String[] extensionList = extensions.split(ServiceExtensionManager.EXTENSION_SEPARATOR);
        for (String extension : extensionList) {
            if (!TextUtils.isEmpty(extension) && extension.trim().length() > 0) {
                result.add(extension);
            }
        }
        return result;
    }

    /**
     * Concatenate set of extensions into a string
     * 
     * @param extensions set of extensions
     * @return String where extensions are concatenated with a ";" separator
     */
    public static String getExtensions(Set<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return "";

        }
        StringBuilder result = new StringBuilder();
        int size = extensions.size();
        for (String extension : extensions) {
            if (extension.trim().length() == 0) {
                --size;
                continue;

            }
            result.append(extension);
            if (--size != 0) {
                // Not last item : add separator
                result.append(EXTENSION_SEPARATOR);
            }
        }
        return result.toString();
    }

}
