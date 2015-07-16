
package com.gsma.rcs.provider.security;

import com.gsma.rcs.provider.security.AuthorizationData.Type;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache implementation for authorization data
 */
@SuppressLint("UseSparseArrays")
public class CacheAuth {

    private Map<String, AuthorizationData> iariMap;

    private Map<Integer, AuthorizationData> uidMap;

    /**
     * Default Constructor
     */
    public CacheAuth() {
        iariMap = new HashMap<String, AuthorizationData>();
        uidMap = new HashMap<Integer, AuthorizationData>();
    }

    /**
     * Add an authorization in the cache
     * 
     * @param authorization
     */
    public void add(AuthorizationData authorization) {
        iariMap.put(authorization.getIari(), authorization);
        if (Type.APPLICATION_ID == authorization.getType()) {
            uidMap.put(authorization.getPackageUid(), authorization);
        }
    }

    /**
     * Get authorization for service
     * 
     * @param serviceId the service ID
     * @return AuthorizationData
     */
    public AuthorizationData getServiceAuth(String serviceId) {
        return iariMap.get(serviceId);
    }

    /**
     * Gets authorization of application
     * 
     * @param uid the application UID
     * @return the application authorization or null if it does not exist
     */
    public AuthorizationData getApplicationAuth(Integer uid) {
        return uidMap.get(uid);
    }

    /**
     * Removes authorization
     * 
     * @param authorization
     */
    public void remove(AuthorizationData authorization) {
        iariMap.remove(authorization.getIari());
        if (Type.APPLICATION_ID == authorization.getType()) {
            uidMap.remove(authorization.getPackageUid());
        }
    }

}
