/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.gsma.rcs.service.permissions;

import android.content.Context;
import android.content.Intent;

/**
 * Author Romain Caron (Orange)
 */
public class PermissionsManager {

    /**
     * Need for asking permissions
     */
    private static volatile PermissionsManager sInstance;
    private boolean mHasPermissions = false;

    /**
     * Private constructor to avoid creating object in external class
     */
    private PermissionsManager() {
    }

    /**
     * Returns the Instance of StartService
     *
     * @return Instance of StartService
     */
    public static PermissionsManager getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (PermissionsManager.class) {
            if (sInstance == null) {
                sInstance = new PermissionsManager();
            }
            return sInstance;
        }
    }

    /**
     * Display the Permissions popup
     *
     * @param context to start the Intent
     * @return boolean, true if all permissions are granted, false if not
     */
    public boolean requestForPermissionsAndWaitResponse(Context context) {
        Intent intent = new Intent(context, PermissionsAlertDialog.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        try {
            synchronized (sInstance) {
                super.wait();
            }
        } catch (InterruptedException e) {
            // nothing to do
        }
        return mHasPermissions;
    }

    /**
     * Callback of the Permissions
     *
     * @param hasPermissions boolean, true if all permissions are granted, false if not
     */
    public void responseReceived(boolean hasPermissions) {
        synchronized (sInstance) {
            mHasPermissions = hasPermissions;
            super.notify();
        }
    }

}
