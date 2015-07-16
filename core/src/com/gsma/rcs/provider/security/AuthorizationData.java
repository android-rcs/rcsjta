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

import android.net.Uri;
import android.util.SparseArray;

/**
 * A class to hold the IARI authorization data.<br>
 * It also defines data to access the authorization table from security provider.
 * 
 * @author yplo6403
 *
 */
/**
 * @author LEMORDANT Philippe
 */
public class AuthorizationData {

    /**
     * Define the type of authorization
     */
    public static enum Type {
        /** Type for Multimedia Session service */
        SERVICE_ID(0),
        /** Type for application */
        APPLICATION_ID(1);

        private int mValue;

        private static SparseArray<Type> mValueToEnum = new SparseArray<Type>();
        static {
            for (Type entry : Type.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private Type(int value) {
            mValue = value;
        }

        /**
         * Get Type as integer value
         * 
         * @return integer
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Get Type from integer value
         * 
         * @param value
         * @return Type
         */
        public static Type valueOf(int value) {
            return mValueToEnum.get(value);
        }
    }

    /**
     * Database URI
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.rcs.security/authorization");

    /**
     * Column name primary key
     * <P>
     * Type: INTEGER AUTO INCREMENTED
     * </P>
     */
    public static final String KEY_BASECOLUMN_ID = "_id";

    /**
     * The name of the column containing the package UID
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String KEY_PACK_UID = "pack_uid";

    /**
     * The name of the column containing the specific part of the IARI (i.e. the service ID or
     * application ID)
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String KEY_IARI = "iari";

    /**
     * The name of the column containing the package name.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String KEY_PACK_NAME = "pack_name";

    /**
     * The name of the column containing the type.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String KEY_TYPE = "type";

    private final Integer mPackageUid;
    private final String mIari;
    private final String mPackageName;
    private final Type mType;

    /**
     * Constructor
     * 
     * @param packageUid
     * @param packageName
     * @param iari
     * @param type
     */
    public AuthorizationData(Integer packageUid, String packageName, String iari, Type type) {
        mPackageUid = packageUid;
        mPackageName = packageName;
        mIari = iari;
        mType = type;
    }

    /**
     * Gets the IARI
     * 
     * @return the IARI
     */
    public String getIari() {
        return mIari;
    }

    /**
     * Gets package name
     * 
     * @return package name
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Gets package UID
     * 
     * @return package UID
     */
    public Integer getPackageUid() {
        return mPackageUid;
    }

    /**
     * Gets type
     * 
     * @return the type
     */
    public Type getType() {
        return mType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mIari == null) ? 0 : mIari.hashCode());
        result = prime * result + ((mPackageName == null) ? 0 : mPackageName.hashCode());
        result = prime * result + ((mPackageUid == null) ? 0 : mPackageUid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AuthorizationData other = (AuthorizationData) obj;
        if (mIari == null) {
            if (other.mIari != null)
                return false;
        } else if (!mIari.equals(other.mIari))
            return false;
        if (mPackageName == null) {
            if (other.mPackageName != null)
                return false;
        } else if (!mPackageName.equals(other.mPackageName))
            return false;
        if (mPackageUid == null) {
            if (other.mPackageUid != null)
                return false;
        } else if (!mPackageUid.equals(other.mPackageUid))
            return false;
        return true;
    }

}
