/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.extension;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.security.AuthorizationData;
import com.gsma.rcs.provider.security.SecurityLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.ServerApiUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.capability.CapabilityService;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A class to update the supported extensions in background.
 * 
 * @author LEMORDANT Philippe
 */
public class SupportedExtensionUpdater implements Runnable {

    private final RcsSettings mRcsSettings;

    private final SecurityLog mSecurityLog;

    private final ExtensionManager mExtensionManager;

    private final ServerApiUtils mServerApiUtils;

    private Integer mUid;

    private String mPackageName;

    private boolean mPackagedRemoved;

    private final PackageManager mPackageManager;

    private final static Logger sLogger = Logger.getLogger(SupportedExtensionUpdater.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param context
     * @param rcsSettings
     * @param securityLog
     * @param extensionManager
     * @param serverApiUtils
     */
    public SupportedExtensionUpdater(Context context, RcsSettings rcsSettings,
            SecurityLog securityLog, ExtensionManager extensionManager,
            ServerApiUtils serverApiUtils) {
        mRcsSettings = rcsSettings;
        mSecurityLog = securityLog;
        mExtensionManager = extensionManager;
        mServerApiUtils = serverApiUtils;
        mPackageManager = context.getPackageManager();
    }

    public SupportedExtensionUpdater(Integer uid, String packageName, boolean packageRemoved,
            Context context, RcsSettings rcsSettings, SecurityLog securityLog,
            ExtensionManager extensionManager, ServerApiUtils serverApiUtils) {
        mRcsSettings = rcsSettings;
        mSecurityLog = securityLog;
        mExtensionManager = extensionManager;
        mServerApiUtils = serverApiUtils;
        mPackageManager = context.getPackageManager();
        mUid = uid;
        mPackageName = packageName;
        mPackagedRemoved = packageRemoved;
    }

    @Override
    public void run() {
        if (sLogger.isActivated()) {
            sLogger.debug("Update supported extensions");
        }
        try {
            if (mPackageName == null) {
                checkSupportedExtensionsForAllPackages();
            } else {
                checkSupportedExtensionsForPackage();
            }
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to update supported extensions!", e);
        } catch (NameNotFoundException e) {
            sLogger.error("Cannot find package: '" + mPackageName + "'!", e);
        }
    }

    private void postCheckSupportedExtensions(Map<AuthorizationData, Integer> authBeforeUpdate,
            Set<AuthorizationData> authAfterUpdate, boolean securityLogHasChanged) {
        /* Remove invalid authorizations */
        authBeforeUpdate.keySet().removeAll(authAfterUpdate);
        for (AuthorizationData authorizationData : authBeforeUpdate.keySet()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Remove authorization for package '"
                        + authorizationData.getPackageName() + "' iari:"
                        + authorizationData.getIari());
            }
            securityLogHasChanged = true;
            mSecurityLog.removeAuthorization(authBeforeUpdate.get(authorizationData),
                    authorizationData);
        }
        if (securityLogHasChanged) {
            /* update RCS Extension for registration */
            mRcsSettings.setSupportedRcsExtensions(mSecurityLog.getSupportedExtensions());

            Core core = Core.getInstance();
            if (mServerApiUtils.isImsConnected() && core != null) {
                core.getImsModule().getSipManager().getNetworkInterface().getRegistrationManager()
                        .restart();
            }
        }
    }

    private void checkSupportedExtensionsForPackage() throws NameNotFoundException {
        boolean isLogActivated = sLogger.isActivated();
        /* Save authorizations before update */
        Map<AuthorizationData, Integer> authBeforeUpdate = mSecurityLog
                .getAuthorizationsByUid(mUid);
        Set<AuthorizationData> authAfterUpdate = new HashSet<AuthorizationData>();
        boolean securityLogHasChanged = false;
        if (mRcsSettings.isExtensionsAllowed()) {
            if (mPackagedRemoved) {
                if (!authBeforeUpdate.isEmpty()) {
                    // Remove the extensions in the supported RCS extensions
                    mExtensionManager.removeAuthorizationsForPackage(mUid);
                    securityLogHasChanged = true;
                }
            } else {
                Map<String, String> extensions = getManagedExtensions(mPackageName);
                if (!mRcsSettings.isExtensionsControlled()) {
                    if (isLogActivated) {
                        sLogger.debug("No control on extensions");
                    }
                    for (Entry<String, String> extensionEntry : extensions.entrySet()) {
                        String iari = extensionEntry.getKey();
                        String filename = extensionEntry.getValue();
                        AuthorizationData authData = new AuthorizationData(mUid, mPackageName,
                                iari, IariUtil.getType(filename));
                        authAfterUpdate.add(authData);
                    }
                } else {
                    /* Check if extensions are supported */
                    Set<AuthorizationData> supportedExts = mExtensionManager.checkExtensions(
                            mPackageManager, mUid, mPackageName, extensions);
                    authAfterUpdate.addAll(supportedExts);
                }
                for (AuthorizationData authorizationData : authAfterUpdate) {
                    if (!authBeforeUpdate.containsKey(authorizationData)) {
                        /* Save new authorizations */
                        securityLogHasChanged = true;
                        mSecurityLog.addAuthorization(authorizationData);
                    }
                }
            }
        } else {
            if (isLogActivated) {
                sLogger.debug("Extensions are NOT allowed");
            }
        }
        postCheckSupportedExtensions(authBeforeUpdate, authAfterUpdate, securityLogHasChanged);
    }

    private void checkSupportedExtensionsForAllPackages() {
        boolean isLogActivated = sLogger.isActivated();
        /* Save authorizations before update */
        Map<AuthorizationData, Integer> authBeforeUpdate = mSecurityLog.getAllAuthorizations();

        Set<AuthorizationData> authAfterUpdate = new HashSet<AuthorizationData>();
        boolean securityLogHasChanged = false;

        if (mRcsSettings.isExtensionsAllowed()) {
            Map<String, Map<String, String>> packageNames = getPackagesManagingExtensions();
            for (Entry<String, Map<String, String>> packageEntry : packageNames.entrySet()) {

                String packageName = packageEntry.getKey();
                Integer uid = mExtensionManager.getUidForPackage(mPackageManager, packageName);
                if (uid == null) {
                    continue;
                }
                Map<String, String> extensions = packageEntry.getValue();
                if (!mRcsSettings.isExtensionsControlled()) {
                    if (isLogActivated) {
                        sLogger.debug("No control on extensions");
                    }
                    for (Entry<String, String> extensionEntry : extensions.entrySet()) {
                        String iari = extensionEntry.getKey();
                        String filename = extensionEntry.getValue();
                        AuthorizationData authData = new AuthorizationData(uid, packageName, iari,
                                IariUtil.getType(filename));
                        authAfterUpdate.add(authData);
                    }
                } else {
                    /* Check if extensions are supported */
                    Set<AuthorizationData> supportedExts = mExtensionManager.checkExtensions(
                            mPackageManager, uid, packageName, extensions);
                    authAfterUpdate.addAll(supportedExts);
                }
            }
            /* Save new authorizations */
            for (AuthorizationData authorizationData : authAfterUpdate) {
                if (!authBeforeUpdate.containsKey(authorizationData)) {
                    securityLogHasChanged = true;
                    mSecurityLog.addAuthorization(authorizationData);
                }
            }
        } else {
            if (isLogActivated) {
                sLogger.debug("Extensions are NOT allowed");
            }
        }
        postCheckSupportedExtensions(authBeforeUpdate, authAfterUpdate, securityLogHasChanged);
    }

    /**
     * Gets packages names and associated map of extensions with associated filename.
     * 
     * @return a map with package names and associated extensions with associated filename.
     */
    private Map<String, Map<String, String>> getPackagesManagingExtensions() {
        Map<String, Map<String, String>> packagesWithExtensions = new HashMap<String, Map<String, String>>();
        List<ApplicationInfo> apps = mPackageManager
                .getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : apps) {
            Bundle appMeta = appInfo.metaData;
            if (appMeta != null) {
                Map<String, String> extensions = mExtensionManager
                        .getServiceIdsFromMetadata(appMeta);

                String applicationId = appMeta.getString(CapabilityService.METADATA_APPLICATION_ID);
                if (applicationId != null) {
                    applicationId = new StringBuilder(IariUtil.EXTENSION_ID_PREFIX).append(
                            applicationId).toString();
                    extensions.put(applicationId, IariUtil.IARI_DOC_NAME_FOR_APP_ID);
                }
                if (!extensions.isEmpty()) {
                    // Save package name
                    packagesWithExtensions.put(appInfo.packageName, extensions);
                }
            }
        }
        return packagesWithExtensions;
    }

    /**
     * Gets managed extensions for package name
     * 
     * @param packageName the package name
     * @return managed extensions associated with their filename
     * @throws NameNotFoundException
     */
    private Map<String, String> getManagedExtensions(String packageName)
            throws NameNotFoundException {
        Map<String, String> extensions = new HashMap<String, String>();
        /* Get extensions associated to the new application */
        ApplicationInfo appInfo = mPackageManager.getApplicationInfo(packageName,
                PackageManager.GET_META_DATA);
        if (appInfo == null) {
            /* No app info */
            return extensions;
        }
        Bundle appMeta = appInfo.metaData;
        if (appMeta == null) {
            /* No app meta */
            return extensions;
        }
        extensions = mExtensionManager.getServiceIdsFromMetadata(appMeta);
        String applicationId = appMeta.getString(CapabilityService.METADATA_APPLICATION_ID);
        if (applicationId != null) {
            applicationId = new StringBuilder(IariUtil.EXTENSION_ID_PREFIX).append(applicationId)
                    .toString();
            extensions.put(applicationId, IariUtil.IARI_DOC_NAME_FOR_APP_ID);
        }
        return extensions;
    }
}
