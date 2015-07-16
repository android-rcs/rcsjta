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

package com.gsma.rcs.provider.security;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.security.AuthorizationData.Type;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * SecurityLog class to manage certificates for IARI range or authorizations for IARI
 * 
 * @author yplo6403
 */
public class SecurityLog {
    /**
     * Current instance
     */
    private static volatile SecurityLog sInstance;

    private CacheAuth mCacheAuth;
    private Map<String, RevocationData> mCacheRev;

    private final LocalContentResolver mLocalContentResolver;

    // authorizations definition
    private static final String[] AUTH_PROJ_IARI = new String[] {
        AuthorizationData.KEY_IARI
    };

    private static final String[] AUTH_PROJECTION_ID = new String[] {
        AuthorizationData.KEY_BASECOLUMN_ID
    };

    private static final String AUTH_WHERE_UID = new StringBuilder(AuthorizationData.KEY_PACK_UID)
            .append("=?").toString();

    private static final String AUTH_WHERE_UID_AND_TYPE_APP = new StringBuilder(
            AuthorizationData.KEY_PACK_UID).append("=? AND ").append(AuthorizationData.KEY_TYPE)
            .append("='").append(Type.APPLICATION_ID.toInt()).append("'").toString();

    private static final String AUTH_WHERE_TYPE_EXT = new StringBuilder(AuthorizationData.KEY_TYPE)
            .append("='").append(Type.SERVICE_ID.toInt()).append("'").toString();

    private static final String AUTH_WHERE_IARI_AND_TYPE = new StringBuilder(
            AuthorizationData.KEY_IARI).append("=? AND ").append(AuthorizationData.KEY_TYPE)
            .append("=?").toString();

    private static final String AUTH_WHERE_IARI_AND_TYPE_EXT = new StringBuilder(
            AuthorizationData.KEY_IARI).append("=? AND ").append(AuthorizationData.KEY_TYPE)
            .append("='").append(Type.SERVICE_ID.toInt()).append("'").toString();

    // revocation definitions
    private static final String[] REV_PROJECTION_ID = new String[] {
        RevocationData.KEY_ID
    };

    private final String REV_WHERE_SERVICEID_CLAUSE = new StringBuilder(
            RevocationData.KEY_SERVICE_ID).append("=?").toString();

    public static final int INVALID_ID = -1;

    private static final Logger logger = Logger.getLogger(SecurityLog.class.getSimpleName());

    /**
     * Gets the instance of SecurityLog singleton
     * 
     * @param localContentResolver
     * @return the instance of SecurityLog singleton
     */
    public static SecurityLog getInstance(LocalContentResolver localContentResolver) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (SecurityLog.class) {
            if (sInstance == null) {
                sInstance = new SecurityLog(localContentResolver);
            }
        }
        return sInstance;
    }

    /**
     * Constructor
     * 
     * @param localContentResolver
     */
    private SecurityLog(LocalContentResolver localContentResolver) {
        mLocalContentResolver = localContentResolver;
        mCacheAuth = new CacheAuth();
        mCacheRev = new HashMap<String, RevocationData>();
    }

    /**
     * Add authorization
     * 
     * @param authData
     */
    public void addAuthorization(AuthorizationData authData) {
        boolean logActivated = logger.isActivated();
        String packageName = authData.getPackageName();
        Integer packageUid = authData.getPackageUid();

        String iari = authData.getIari();
        ContentValues values = new ContentValues();
        values.put(AuthorizationData.KEY_IARI, iari);
        Type type = authData.getType();
        values.put(AuthorizationData.KEY_TYPE, type.toInt());
        Integer id = getAuthorizationIdByIariAndType(iari, type);
        mCacheAuth.add(authData);
        if (INVALID_ID == id) {
            if (logActivated) {
                logger.debug(new StringBuilder("Add authorization for package '")
                        .append(packageName).append("'/").append(packageUid).append(" iari:")
                        .append(iari).toString());
            }
            values.put(AuthorizationData.KEY_PACK_NAME, packageName);
            values.put(AuthorizationData.KEY_PACK_UID, packageUid);
            mLocalContentResolver.insert(AuthorizationData.CONTENT_URI, values);
            return;
        }
        if (logActivated) {
            logger.debug(new StringBuilder("Update authorization for package '")
                    .append(packageName).append("'/").append(packageUid).append(" iari:")
                    .append(iari).toString());
        }
        Uri uri = Uri.withAppendedPath(AuthorizationData.CONTENT_URI, id.toString());
        mLocalContentResolver.update(uri, values, null, null);
    }

    /**
     * Add authorization
     * 
     * @param revocation
     */
    public void addRevocation(RevocationData revocation) {
        boolean logActivated = logger.isActivated();
        ContentValues values = new ContentValues();
        String serviceId = revocation.getServiceId();
        long duration = revocation.getDuration();
        values.put(RevocationData.KEY_DURATION, duration);
        values.put(RevocationData.KEY_SERVICE_ID, serviceId);

        Integer id = getIdForRevocation(serviceId);
        mCacheRev.put(revocation.getServiceId(), revocation);
        if (INVALID_ID == id) {
            if (logActivated) {
                logger.debug("Add revocation for serviceId '" + serviceId + "' duration="
                        + duration);
            }
            mLocalContentResolver.insert(RevocationData.CONTENT_URI, values);
            return;
        }
        if (logActivated) {
            logger.debug("Update revocation for serviceId '" + serviceId + "' duration=" + duration);
        }
        Uri uri = Uri.withAppendedPath(RevocationData.CONTENT_URI, id.toString());
        mLocalContentResolver.update(uri, values, null, null);
    }

    /**
     * Remove a authorization
     * 
     * @param id the row ID
     * @param authorizationData
     * @return The number of rows deleted.
     */
    public int removeAuthorization(int id, AuthorizationData authorizationData) {
        mCacheAuth.remove(authorizationData);
        Uri uri = Uri.withAppendedPath(AuthorizationData.CONTENT_URI, Integer.toString(id));
        return mLocalContentResolver.delete(uri, null, null);
    }

    /**
     * Remove a revocation
     * 
     * @param serviceId
     * @return The number of rows deleted.
     */
    public int removeRevocation(String serviceId) {
        if (logger.isActivated()) {
            logger.debug("Remove revocation for serviceId '".concat(serviceId));
        }

        mCacheRev.remove(serviceId);
        return mLocalContentResolver.delete(RevocationData.CONTENT_URI, REV_WHERE_SERVICEID_CLAUSE,
                new String[] {
                    serviceId
                });
    }

    /**
     * Get all authorizations
     * 
     * @return a map which key set is the AuthorizationData instance and the value set is the row ID
     */
    public Map<AuthorizationData, Integer> getAllAuthorizations() {
        Map<AuthorizationData, Integer> authorizations = new HashMap<AuthorizationData, Integer>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(AuthorizationData.CONTENT_URI, null, null, null,
                    null);
            CursorUtil.assertCursorIsNotNull(cursor, AuthorizationData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return authorizations;
            }
            int idColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_BASECOLUMN_ID);
            int packageColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_NAME);
            int iariColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_IARI);
            int extTypeColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_TYPE);
            int packageUidColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_UID);

            do {
                String iari = cursor.getString(iariColumnIdx);
                Integer extType = cursor.getInt(extTypeColumnIdx);
                String packageName = cursor.getString(packageColumnIdx);
                Integer packageUid = cursor.getInt(packageUidColumnIdx);
                Integer id = cursor.getInt(idColumnIdx);

                AuthorizationData ad = new AuthorizationData(packageUid, packageName, iari,
                        Type.valueOf(extType));
                mCacheAuth.add(ad);
                authorizations.put(ad, id);
            } while (cursor.moveToNext());
            return authorizations;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Gets service authorization.
     * 
     * @param serviceId
     * @return AuthorizationData or null if there is no authorization
     */
    public AuthorizationData getServiceAuthorization(String serviceId) {
        AuthorizationData authorizationData = mCacheAuth.getServiceAuth(serviceId);
        if (authorizationData != null) {
            return authorizationData;
        }
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(AuthorizationData.CONTENT_URI, null,
                    AUTH_WHERE_IARI_AND_TYPE_EXT, new String[] {
                        serviceId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, AuthorizationData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            int packageColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_NAME);
            int extTypeColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_TYPE);
            int uidColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_UID);

            String packageName = cursor.getString(packageColumnIdx);
            Integer extType = cursor.getInt(extTypeColumnIdx);
            Integer uid = cursor.getInt(uidColumnIdx);

            authorizationData = new AuthorizationData(uid, packageName, serviceId,
                    Type.valueOf(extType));
            mCacheAuth.add(authorizationData);
            return authorizationData;

        } finally {
            CursorUtil.close(cursor);
        }

    }

    /**
     * Gets application authorization.
     * 
     * @param packageUid
     * @return AuthorizationData or null if there is no authorization for this package UID
     */
    public AuthorizationData getApplicationAuthorization(Integer packageUid) {
        AuthorizationData auth = mCacheAuth.getApplicationAuth(packageUid);
        if (auth != null) {
            return auth;
        }
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(AuthorizationData.CONTENT_URI, null,
                    AUTH_WHERE_UID_AND_TYPE_APP, new String[] {
                        String.valueOf(packageUid)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, AuthorizationData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            int packageColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_NAME);
            int iariColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_IARI);

            String packageName = cursor.getString(packageColumnIdx);
            String iari = cursor.getString(iariColumnIdx);
            auth = new AuthorizationData(packageUid, packageName, iari, Type.APPLICATION_ID);
            mCacheAuth.add(auth);
            return auth;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get authorization ID for a IARI
     * 
     * @param iari
     * @param type
     * @return id
     */
    public Integer getAuthorizationIdByIariAndType(String iari, Type type) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(AuthorizationData.CONTENT_URI, AUTH_PROJECTION_ID,
                    AUTH_WHERE_IARI_AND_TYPE, new String[] {
                            iari, String.valueOf(type.toInt())
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, AuthorizationData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                int idColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_BASECOLUMN_ID);
                return cursor.getInt(idColumnIdx);
            }
            return INVALID_ID;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Removes authorizations for a package UID
     * 
     * @param packageUid
     */
    public void removeAuthorizationsForPackage(Integer packageUid) {
        AuthorizationData appAuth = mCacheAuth.getApplicationAuth(packageUid);
        if (appAuth != null) {
            mCacheAuth.remove(appAuth);
        }
        mLocalContentResolver.delete(AuthorizationData.CONTENT_URI, AUTH_WHERE_UID, new String[] {
            String.valueOf(packageUid)
        });
    }

    /**
     * Get all supported extensions as serviceId
     * 
     * @return set of supported extensions
     */
    public Set<String> getSupportedExtensions() {
        Set<String> supportedExtensions = new HashSet<String>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(AuthorizationData.CONTENT_URI, AUTH_PROJ_IARI,
                    AUTH_WHERE_TYPE_EXT, null, null);
            CursorUtil.assertCursorIsNotNull(cursor, AuthorizationData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return supportedExtensions;
            }
            int iariColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_IARI);
            do {
                supportedExtensions.add(cursor.getString(iariColumnIdx));
            } while (cursor.moveToNext());
            /* remove revoked extension from results */
            for (Iterator<String> iterator = supportedExtensions.iterator(); iterator.hasNext();) {
                String serviceId = iterator.next();
                if (getRevocationByServiceId(serviceId) != null) {
                    iterator.remove();
                }
            }
            return supportedExtensions;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get row ID for revocation
     * 
     * @param serviceId
     * @return id or INVALID_ID if not found
     */
    public int getIdForRevocation(String serviceId) {
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(RevocationData.CONTENT_URI, REV_PROJECTION_ID,
                    REV_WHERE_SERVICEID_CLAUSE, new String[] {
                        serviceId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, RevocationData.CONTENT_URI);
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(RevocationData.KEY_ID));
            }
            return INVALID_ID;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Get revocation by IARI
     * 
     * @param serviceId
     * @return RevocationData
     */
    public RevocationData getRevocationByServiceId(String serviceId) {
        RevocationData revocationData = mCacheRev.get(serviceId);
        if (revocationData != null) {
            return revocationData;
        }
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(RevocationData.CONTENT_URI, null,
                    REV_WHERE_SERVICEID_CLAUSE, new String[] {
                        serviceId
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, RevocationData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return null;
            }
            int durationIdx = cursor.getColumnIndexOrThrow(RevocationData.KEY_DURATION);
            Long duration = cursor.getLong(durationIdx);
            revocationData = new RevocationData(serviceId, duration);
            mCacheRev.put(serviceId, revocationData);
            return revocationData;

        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Gets authorizations by UID
     * 
     * @param packageUid the package UID
     * @return a map which key set is the AuthorizationData instance and the value set is the row ID
     */
    public Map<AuthorizationData, Integer> getAuthorizationsByUid(Integer packageUid) {
        Map<AuthorizationData, Integer> authorizations = new HashMap<AuthorizationData, Integer>();
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(AuthorizationData.CONTENT_URI, null,
                    AUTH_WHERE_UID, new String[] {
                        String.valueOf(packageUid)
                    }, null);
            CursorUtil.assertCursorIsNotNull(cursor, AuthorizationData.CONTENT_URI);
            if (!cursor.moveToFirst()) {
                return authorizations;
            }
            int idColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_BASECOLUMN_ID);
            int packageColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_NAME);
            int iariColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_IARI);
            int extTypeColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_TYPE);

            do {
                String iari = cursor.getString(iariColumnIdx);
                Integer extType = cursor.getInt(extTypeColumnIdx);
                String packageName = cursor.getString(packageColumnIdx);
                Integer id = cursor.getInt(idColumnIdx);

                AuthorizationData ad = new AuthorizationData(packageUid, packageName, iari,
                        Type.valueOf(extType));
                mCacheAuth.add(ad);
                authorizations.put(ad, id);
            } while (cursor.moveToNext());
            return authorizations;

        } finally {
            CursorUtil.close(cursor);
        }
    }
}
