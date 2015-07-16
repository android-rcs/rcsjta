
package com.gsma.rcs.security;

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
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.security.AuthorizationData;
import com.gsma.rcs.provider.security.AuthorizationData.Type;
import com.gsma.rcs.provider.security.RevocationData;
import com.gsma.rcs.provider.security.SecurityLog;

import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SecurityLibTest extends AndroidTestCase {

    private static final String[] AUTH_PROJECTION_ID = new String[] {
        AuthorizationData.KEY_BASECOLUMN_ID
    };
    private static final String AUTH_WHERE_UID_IARI_CLAUSE = new StringBuilder(
            AuthorizationData.KEY_PACK_UID).append("=? AND ").append(AuthorizationData.KEY_IARI)
            .append("=?").toString();

    public static final int INVALID_ID = -1;

    /**
     * Get authorization by id
     * 
     * @param contentResolver
     * @param id
     * @return AuthorizationData
     */
    public AuthorizationData getAuthorizationById(LocalContentResolver contentResolver, int id) {
        Cursor cursor = null;
        AuthorizationData authorizationData = null;
        try {
            Uri uri = Uri.withAppendedPath(AuthorizationData.CONTENT_URI, Integer.toString(id));
            cursor = contentResolver.query(uri, null, null, null, null);
            if (!cursor.moveToFirst()) {
                return null;
            }
            int packageColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_NAME);
            int iariColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_IARI);
            int packageUidColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_UID);
            int extTypeColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_TYPE);

            String iari = cursor.getString(iariColumnIdx);
            String packageName = cursor.getString(packageColumnIdx);
            Integer packageUid = cursor.getInt(packageUidColumnIdx);
            Integer extType = cursor.getInt(extTypeColumnIdx);
            authorizationData = new AuthorizationData(packageUid, packageName, iari,
                    Type.valueOf(extType));
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return authorizationData;
    }

    /**
     * Get row ID for authorization
     * 
     * @param packageUid
     * @param iari
     * @return id or INVALID_ID if not found
     */
    public int getIdForPackageUidAndIari(LocalContentResolver contentResolver, Integer packageUid,
            String iari) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(AuthorizationData.CONTENT_URI, AUTH_PROJECTION_ID,
                    AUTH_WHERE_UID_IARI_CLAUSE, new String[] {
                            String.valueOf(packageUid), iari
                    }, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor
                        .getColumnIndexOrThrow(AuthorizationData.KEY_BASECOLUMN_ID));
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return INVALID_ID;
    }

    /**
     * Remove all authorizations
     * 
     * @param contentResolver
     * @return The number of rows deleted.
     */
    void removeAllAuthorizations(LocalContentResolver contentResolver) {
        SecurityLog securityLog = SecurityLog.getInstance(contentResolver);
        Iterator<Entry<AuthorizationData, Integer>> iter = securityLog.getAllAuthorizations()
                .entrySet().iterator();
        while (iter.hasNext()) {
            Entry<AuthorizationData, Integer> entry = iter.next();
            securityLog.removeAuthorization(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Get all revocations
     * 
     * @return a map which key set is the RevocationData instance and the value set is the row IDs
     */
    public Map<RevocationData, Integer> getAllRevocations(LocalContentResolver contentResolver) {
        Map<RevocationData, Integer> result = new HashMap<RevocationData, Integer>();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(RevocationData.CONTENT_URI, null, null, null, null);
            if (!cursor.moveToFirst()) {
                return result;
            }
            int idColumnIdx = cursor.getColumnIndexOrThrow(RevocationData.KEY_ID);
            int iariColumnIdx = cursor.getColumnIndexOrThrow(RevocationData.KEY_SERVICE_ID);
            int durationColumnIdx = cursor.getColumnIndexOrThrow(RevocationData.KEY_DURATION);

            String iari = null;
            Long duration = null;
            Integer id = null;

            do {
                iari = cursor.getString(iariColumnIdx);
                duration = cursor.getLong(durationColumnIdx);
                id = cursor.getInt(idColumnIdx);
                RevocationData ad = new RevocationData(iari, duration);
                result.put(ad, id);
            } while (cursor.moveToNext());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * Remove all Revocations
     * 
     * @param contentResolver
     * @return The number of rows deleted.
     */
    int removeAllRevocations(LocalContentResolver contentResolver) {
        return contentResolver.delete(RevocationData.CONTENT_URI, null, null);
    }

}
