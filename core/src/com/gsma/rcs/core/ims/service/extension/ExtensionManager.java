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

import com.gsma.iariauth.validator.IARIAuthDocument;
import com.gsma.iariauth.validator.PackageProcessor;
import com.gsma.iariauth.validator.ProcessingResult;
import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.security.AuthorizationData;
import com.gsma.rcs.provider.security.RevocationData;
import com.gsma.rcs.provider.security.SecurityLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.ExtensionPolicy;
import com.gsma.rcs.service.api.ServerApiPermissionDeniedException;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.capability.CapabilityService;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Service extension manager which adds supported extension after having verified some authorization
 * rules.
 * 
 * @author Jean-Marc AUFFRET
 * @author P.LEMORDANT
 * @author F.ABOT
 */
public class ExtensionManager {

    private final static String LEADING_ZERO = "0";

    private final static String EXTENSION_SEPARATOR = ";";

    private final static int MILLISECONDS = 1000;

    /**
     * Singleton of ExtensionManager
     */
    private static volatile ExtensionManager sInstance;

    private final static Logger sLogger = Logger.getLogger(ExtensionManager.class.getSimpleName());

    final private RcsSettings mRcsSettings;

    final private SecurityLog mSecurityLog;

    final private Context mContext;

    private final String mRcsFingerPrint;

    private final Map<Integer, String> mCacheFingerprint;

    /**
     * Constructor
     */
    @SuppressLint("UseSparseArrays")
    private ExtensionManager(Context context, RcsSettings rcsSettings, SecurityLog securityLog) {
        mRcsSettings = rcsSettings;
        mSecurityLog = securityLog;
        mContext = context;
        mRcsFingerPrint = getFingerprint(mContext, mContext.getApplicationContext()
                .getPackageName());
        mCacheFingerprint = new HashMap<Integer, String>();
    }

    /**
     * Get the instance of ExtensionManager.
     *
     * @param context
     * @param rcsSettings
     * @param securityLog
     * @return the singleton instance.
     */
    public static ExtensionManager getInstance(Context context, RcsSettings rcsSettings,
            SecurityLog securityLog) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ExtensionManager.class) {
            if (sInstance == null) {
                sInstance = new ExtensionManager(context, rcsSettings, securityLog);
            }
        }
        return sInstance;
    }

    /**
     * Check if the extensions are valid.
     *
     * @param pkgManager
     * @param uid
     * @param pkgName
     * @param extensions map of extensions to validate associated with the IARI authorization
     *            filename
     * @return Set of authorization data
     */
    public Set<AuthorizationData> checkExtensions(PackageManager pkgManager, Integer uid,
            String pkgName, Map<String, String> extensions) {
        Set<AuthorizationData> result = new HashSet<AuthorizationData>();
        boolean isLogActivated = sLogger.isActivated();
        /* Check each new extension */
        for (Entry<String, String> extensionEntry : extensions.entrySet()) {
            String extension = extensionEntry.getKey();
            String iariDocFilename = extensionEntry.getValue();
            IARIAuthDocument authDocument = getExtensionAuthorizedBySecurity(pkgManager, pkgName,
                    extension, iariDocFilename);
            if (authDocument == null) {
                if (isLogActivated) {
                    sLogger.warn(new StringBuilder("Extension '").append(extension)
                            .append("' CANNOT be added: no authorized document").toString());
                }
                continue;
            }

            if (!IariUtil.isValidIARI(authDocument.iari)) {
                if (isLogActivated) {
                    sLogger.warn(new StringBuilder("IARI '").append(authDocument.iari)
                            .append("' CANNOT be added: NOT a valid extension").toString());
                }
                continue;
            }
            if (!isNativeApplication(uid)
                    && ExtensionPolicy.ONLY_NATIVE == mRcsSettings.getExtensionspolicy()) {
                if (isLogActivated) {
                    sLogger.warn(new StringBuilder("IARI '").append(authDocument.iari)
                            .append("' CANNOT be added: self signed app are not authorized")
                            .toString());
                }
                continue;
            }
            String extId = IariUtil.getExtensionId(authDocument.iari);
            if (!extension.equals(extId)) {
                if (isLogActivated) {
                    sLogger.warn(new StringBuilder("IARI '")
                            .append(extension)
                            .append("' in manifest file does not match document in assets '"
                                    + extId + "'").toString());
                }
                continue;
            }
            /* Add the extension in the supported list if authorized and not yet in the list */
            AuthorizationData authData = new AuthorizationData(uid, authDocument.packageName,
                    extension, IariUtil.getType(iariDocFilename));
            result.add(authData);
            if (isLogActivated) {
                if (isLogActivated) {
                    sLogger.debug(new StringBuilder("Extension '").append(extension)
                            .append("' is authorized. IARI tag: ").append(authDocument.iari)
                            .toString());
                }
            }
        }
        return result;
    }

    /**
     * Save authorizations in authorization table for caching
     * 
     * @param authorizationDatas collection of authorizations
     */
    private void saveAuthorizations(Collection<AuthorizationData> authorizationDatas) {
        for (AuthorizationData authData : authorizationDatas) {
            mSecurityLog.addAuthorization(authData);
        }
    }

    /**
     * Save authorizations in authorization table for caching.<br>
     * This method is used when authorization data are not controlled.
     * 
     * @param uid
     * @param pkgName
     * @param extensions set of extensions
     */
    private void saveAuthorizations(Integer uid, String pkgName, Map<String, String> extensions) {
        for (Entry<String, String> extensionEntry : extensions.entrySet()) {
            /* Save supported extension in database */
            String filename = extensionEntry.getValue();
            String iari = extensionEntry.getKey();
            AuthorizationData authData = null;
            authData = new AuthorizationData(uid, pkgName, iari, IariUtil.getType(filename));
            mSecurityLog.addAuthorization(authData);
        }
    }

    /**
     * Remove authorizations for package
     *
     * @param packageUid
     */
    public void removeAuthorizationsForPackage(Integer packageUid) {
        /* remove the fingerprint from cache */
        mCacheFingerprint.remove(packageUid);
        mSecurityLog.removeAuthorizationsForPackage(packageUid);
    }

    /**
     * Add extensions if supported
     * 
     * @param pkgManager The package manager
     * @param uid The package UID
     * @param pkgName The package name
     * @param extensions Set of extensions
     */
    public void addSupportedExtensions(PackageManager pkgManager, Integer uid, String pkgName,
            Map<String, String> extensions) {
        if (!mRcsSettings.isExtensionsControlled() || isNativeApplication(uid)) {
            if (sLogger.isActivated()) {
                sLogger.debug("No control on extensions");
            }
            saveAuthorizations(uid, pkgName, extensions);
            return;
        }
        /* Check if extensions are supported */
        Set<AuthorizationData> supportedExts = checkExtensions(pkgManager, uid, pkgName, extensions);
        /*
         * Save IARI Authorization document in cache to avoid having to re-process the signature
         * each time the application is loaded.
         */
        saveAuthorizations(supportedExts);
    }

    /**
     * Extract set of extensions from String
     *
     * @param extensions String where extensions are concatenated with a ";" separator
     * @return the set of extensions
     */
    public static Set<String> getExtensions(String extensions) {
        Set<String> result = new HashSet<String>();
        if (TextUtils.isEmpty(extensions)) {
            return result;
        }
        String[] extensionList = extensions.split(ExtensionManager.EXTENSION_SEPARATOR);
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
                /* Not last item : add separator */
                result.append(EXTENSION_SEPARATOR);
            }
        }
        return result.toString();
    }

    /**
     * Get authorized extensions.<br>
     * NB: there can be at most one IARI for a given extension by app
     * 
     * @param pkgManager the app's package manager
     * @param pkgName Package name
     * @param extension Extension ID
     * @param iariResource the IARI authorization document filename
     * @return IARIAuthDocument or null if not authorized
     */
    private IARIAuthDocument getExtensionAuthorizedBySecurity(PackageManager pkgManager,
            String pkgName, String extension, String iariResource) {
        boolean isLogActivated = sLogger.isActivated();
        try {
            if (isLogActivated) {
                sLogger.debug(new StringBuilder("Check extension ").append(extension)
                        .append(" for package ").append(pkgName).toString());
            }

            PackageInfo pkg = pkgManager.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
            Signature[] signs = pkg.signatures;

            if (signs.length == 0) {
                if (isLogActivated) {
                    sLogger.debug("Extension is not authorized: no signature found");
                }
                return null;

            }
            String sha1Sign = getFingerprint(signs[0].toByteArray());
            if (isLogActivated) {
                sLogger.debug("Check application fingerprint: ".concat(sha1Sign));
            }
            PackageProcessor processor = new PackageProcessor(pkgName, sha1Sign);

            InputStream iariDocument = getIariDocumentFromAssets(pkgManager, pkgName, iariResource);
            /* Is IARI document resource found ? */
            if (iariDocument == null) {
                if (isLogActivated) {
                    sLogger.warn("Failed to find IARI document for ".concat(extension));
                }
                return null;

            }
            if (isLogActivated) {
                sLogger.debug("IARI document found for ".concat(extension));
            }

            try {
                ProcessingResult result = processor.processIARIauthorization(iariDocument);
                if (ProcessingResult.STATUS_OK == result.getStatus()) {
                    return result.getAuthDocument();
                    // ---
                }
                if (isLogActivated) {
                    sLogger.debug(new StringBuilder("Extension '").append(extension)
                            .append("' is not authorized: ").append(result.getStatus()).append(" ")
                            .append(result.getError()).toString());
                }
            } catch (Exception e) {
                if (isLogActivated) {
                    sLogger.error("Exception raised when processing IARI doc=".concat(extension), e);
                }
            } finally {
                iariDocument.close();
            }
        } catch (Exception e) {
            if (isLogActivated) {
                sLogger.error("Internal exception", e);
            }
        }
        return null;
    }

    /**
     * Get IARI authorization document from assets
     * 
     * @param pkgManager
     * @param pkgName
     * @param iariResourceName
     * @return InputStream or null if not found
     */
    private InputStream getIariDocumentFromAssets(PackageManager pkgManager, String pkgName,
            String iariResourceName) {
        try {
            Resources res = pkgManager.getResourcesForApplication(pkgName);
            AssetManager am = res.getAssets();
            return am.open(iariResourceName);

        } catch (IOException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Cannot get IARI document from assets", e);
            }
        } catch (NameNotFoundException e) {
            if (sLogger.isActivated()) {
                sLogger.error("IARI authorization doc no found", e);
            }
        }
        return null;
    }

    /**
     * Returns the fingerprint of a certificate
     * 
     * @param cert Certificate
     * @return String as xx:yy:zz
     * @throws NoSuchAlgorithmException
     */
    private String getFingerprint(byte[] cert) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(cert);
        byte[] digest = md.digest();

        String toRet = "";
        for (int i = 0; i < digest.length; i++) {
            if (i != 0)
                toRet = toRet.concat(":");
            int b = digest[i] & 0xff;
            String hex = Integer.toHexString(b);
            if (hex.length() == 1)
                toRet = toRet.concat(LEADING_ZERO);
            toRet = toRet.concat(hex);
        }
        return toRet.toUpperCase();
    }

    /**
     * Test API permission for a packageUid and an extension type
     * 
     * @param packageUid
     * @throws ServerApiPermissionDeniedException
     */
    public void testApiPermission(Integer packageUid) throws ServerApiPermissionDeniedException {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("testApiPermission : packageUid : ".concat(String.valueOf(packageUid)));
        }
        if (!mRcsSettings.isExtensionsControlled()) {
            if (logActivated) {
                sLogger.debug("  --> No control on extensions");
            }
            return;
        }

        AuthorizationData authorization = mSecurityLog.getApplicationAuthorization(packageUid);
        if (authorization == null) {
            if (logActivated) {
                sLogger.debug("  --> The application has no valid authorization");
            }
            throw new ServerApiPermissionDeniedException(new StringBuilder("Application uid '")
                    .append(packageUid).append("' is not authorized").toString());
        }

        String extensionId = authorization.getIari();
        RevocationData revocation = mSecurityLog.getRevocationByServiceId(extensionId);
        if (revocation != null) {
            if (logActivated) {
                sLogger.debug("  --> ServiceId is revoked : ".concat(extensionId));
            }
            throw new ServerApiPermissionDeniedException(new StringBuilder("Extension ")
                    .append(extensionId).append(" is not authorized").toString());
        }
    }

    /**
     * Test API permission for a packageUid and a serviceId. <br>
     * This method should be called only for multimedia session.
     * 
     * @param packageUid
     * @param serviceId
     * @throws ServerApiPermissionDeniedException
     */
    public void testServicePermission(Integer packageUid, String serviceId)
            throws ServerApiPermissionDeniedException {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug(new StringBuilder("testExtensionPermission : packageUid / serviceId  : ")
                    .append(packageUid).append("/").append(serviceId).toString());
        }
        if (!mRcsSettings.isExtensionsControlled()) {
            if (logActivated) {
                sLogger.debug("  --> No control on extensions");
            }
            return;
        }

        RevocationData revocation = mSecurityLog.getRevocationByServiceId(serviceId);
        if (revocation != null) {
            if (logActivated) {
                sLogger.debug("  --> ServiceId is revoked : ".concat(serviceId));
            }
            throw new ServerApiPermissionDeniedException(new StringBuilder("Extension ")
                    .append(serviceId).append(" is not authorized").toString());
        }

        AuthorizationData authorization = mSecurityLog.getServiceAuthorization(serviceId);
        if (authorization != null && authorization.getPackageUid().equals(packageUid)) {
            return;
        }

        if (logActivated) {
            sLogger.debug("    --> Extension is not authorized : ".concat(serviceId));
        }
        throw new ServerApiPermissionDeniedException(new StringBuilder("Extension ")
                .append(serviceId).append(" is not authorized").toString());
    }

    /**
     * Update supported extensions<br>
     * Updates are queued in order to be serialized.
     */
    public void updateSupportedExtensions() {
        Core core = Core.getInstance();
        if (core != null) {
            core.getListener().checkIfSupportedExtensionsHaveChanged();
        }
    }

    /**
     * Returns the UID for the installed application
     * 
     * @param packageManager
     * @param packageName
     * @return the package UID
     */
    public Integer getUidForPackage(PackageManager packageManager, String packageName) {
        try {
            return packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).uid;
        } catch (NameNotFoundException e) {
            if (sLogger.isActivated()) {
                sLogger.error(new StringBuilder(
                        "Package name not found in currently installed applications : ").append(
                        packageName).toString());
            }
            return null;
        }
    }

    /**
     * Return the fingerprint from the PackageManager for an application package
     * 
     * @param context
     * @param packageName
     * @return
     */
    private String getFingerprint(Context context, String packageName) {
        try {
            Signature[] sig;
            sig = mContext.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES).signatures;
            if (sig != null && sig.length > 0) {
                return getFingerprint(sig[0].toByteArray());
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can not get fingerprint for Rcs application", e);
            }
        }
        return null;
    }

    /**
     * Return if the application is a "native" or "third party" application It compares client
     * application fingerprint with the RCS application fingerprint
     * 
     * @param packageUid of the application
     * @return true for native app, false otherwise
     */
    public boolean isNativeApplication(Integer packageUid) {
        if (!mCacheFingerprint.containsKey(packageUid)) {
            String[] packageNames = mContext.getPackageManager().getPackagesForUid(packageUid);
            if (packageNames != null && packageNames.length > 0) {
                String clientAppFingerprint = getFingerprint(mContext, packageNames[0]);
                mCacheFingerprint.put(packageUid, clientAppFingerprint);
            }
        }
        return mRcsFingerPrint.equals(mCacheFingerprint.get(packageUid));
    }

    /**
     * Get the Application Id (IARI) for Third party application
     * 
     * @param packageUid
     * @return applicationId or null if no control on extensions
     */
    public String getApplicationId(Integer packageUid) {
        boolean logActivated = sLogger.isActivated();
        if (!mRcsSettings.isExtensionsControlled()) {
            if (logActivated) {
                sLogger.debug("No control on applicationId");
            }
            return null;
        }
        AuthorizationData auth = mSecurityLog.getApplicationAuthorization(packageUid);
        if (auth != null) {
            return auth.getIari();
        }
        return null;
    }

    /**
     * Revoke extensions
     * 
     * @param extensions Set of extensions
     */
    public void revokeExtensions(Set<String> extensions) {
        for (String extension : extensions) {
            String data[] = extension.split(",");
            if (data.length < 2) {
                continue;
            }
            String iari = data[0];
            String durationStr = data[1];

            String serviceId = IariUtil.getExtensionId(iari.trim());
            long duration = Long.parseLong(durationStr.trim());
            if (duration >= 0) {
                duration *= MILLISECONDS;
                mSecurityLog.addRevocation(new RevocationData(serviceId, duration));
            } else {
                mSecurityLog.removeRevocation(serviceId);
            }
        }
    }

    /**
     * Gets the service IDs from meta data
     * 
     * @param metaData
     * @return the map of service IDs associated with their filename
     */
    public Map<String, String> getServiceIdsFromMetadata(Bundle metaData) {
        Map<String, String> extensions = new HashMap<String, String>();
        int i = 0;
        String metaDataValue;
        do {
            String metaDataKey = new StringBuilder(CapabilityService.INTENT_EXTENSIONS).append(".")
                    .append(i).toString();
            metaDataValue = metaData.getString(metaDataKey);
            if (metaDataValue != null) {
                String extensionId = new StringBuilder(IariUtil.EXTENSION_ID_PREFIX).append(
                        metaDataValue).toString();
                String filename = new StringBuilder(IariUtil.IARI_DOC_NAME_FOR_EXT_PREFIX)
                        .append(i).append(IariUtil.IARI_DOC_NAME_FOR_EXT_POSTFIX).toString();
                extensions.put(extensionId, filename);
            }
            i++;
        } while (metaDataValue != null);
        return extensions;
    }

}
