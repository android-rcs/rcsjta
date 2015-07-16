
package com.gsma.rcs.provider.security;

import android.net.Uri;

/**
 * A class for representing a serviceId revocation. The revocation is based on a duration. If the
 * duration equals: -1 : the serviceId is authorized 0 : the serviceId is revoked for ever >0 : the
 * serviceId is revoked until the duration + currentTime
 */
public class RevocationData {

    /**
     * Database URI
     */
    public static final Uri CONTENT_URI = Uri.parse("content://com.gsma.rcs.security/revocation");

    /**
     * Column name primary key
     * <P>
     * Type: INTEGER AUTO INCREMENTED
     * </P>
     */
    public static final String KEY_ID = "_id";

    /**
     * The name of the column containing the serviceId tag as the unique ID of certificate
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String KEY_SERVICE_ID = "serviceId";

    /**
     * The name of the column containing the revocation duration
     * <P>
     * Type: LONG
     * </P>
     */
    public static final String KEY_DURATION = "duration";

    private final static int REVOKED_INFINITE = 0;

    private final String mServiceId;
    private final long mDuration;

    /**
     * Constructor
     * 
     * @param serviceId
     * @param duration in ms
     */
    public RevocationData(String serviceId, long duration) {
        if (duration > 0) {
            duration = System.currentTimeMillis() + duration;
        }
        mServiceId = serviceId;
        mDuration = duration;
    }

    /**
     * Verify if infinite revoked
     * 
     * @return boolean
     */
    public boolean isRevokedInfinite() {
        return REVOKED_INFINITE == mDuration;
    }

    /**
     * Get duration parameter in ms
     * 
     * @return long duration
     */
    public long getDuration() {
        return mDuration;
    }

    /**
     * Get serviceId parameter
     * 
     * @return String serviceId
     */
    public String getServiceId() {
        return mServiceId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (mDuration ^ (mDuration >>> 32));
        result = prime * result + ((mServiceId == null) ? 0 : mServiceId.hashCode());
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
        RevocationData other = (RevocationData) obj;
        if (mDuration != other.mDuration)
            return false;
        if (mServiceId == null) {
            if (other.mServiceId != null)
                return false;
        } else if (!mServiceId.equals(other.mServiceId))
            return false;
        return true;
    }

}
