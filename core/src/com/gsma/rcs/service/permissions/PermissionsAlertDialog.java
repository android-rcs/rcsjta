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

import com.gsma.rcs.R;
import com.gsma.rcs.utils.logger.Logger;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PermissionsAlertDialog extends Activity {

    /**
     * List of permissions needed for service Just need to ask one permission per dangerous group
     */
    private static final Set<String> sAllPermissionsList = new HashSet<>(Arrays.asList(
            Manifest.permission.READ_CONTACTS, Manifest.permission.READ_SMS,
            Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE));

    private static final int MY_PERMISSION_REQUEST_ALL = 5428;
    private Set<String> mPermissionsToAsk;

    private static final Logger sLogger = Logger.getLogger(PermissionsAlertDialog.class
            .getSimpleName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_alert_dialog);
        askPermissions();
    }

    /**
     * Main function to ask permissions
     */
    public void askPermissions() {
        mPermissionsToAsk = getNotGrantedPermissions();
        if (mPermissionsToAsk.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(mPermissionsToAsk.toArray(new String[mPermissionsToAsk.size()]),
                    MY_PERMISSION_REQUEST_ALL);
        } else {
            sendResponse(true);
        }
    }

    /**
     * Check all permissions's status
     *
     * @return Set of permissions that are not granted
     */
    private Set<String> getNotGrantedPermissions() {
        Set<String> permissionsToAsk = new HashSet<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : PermissionsAlertDialog.sAllPermissionsList) {
                if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(permission)) {
                    permissionsToAsk.add(permission);
                }
            }
        }
        return permissionsToAsk;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_ALL:
                Set<String> grantedPermissions = new HashSet<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        grantedPermissions.add(permissions[i]);

                    } else {
                        if (sLogger.isActivated()) {
                            sLogger.warn("Permission Denied: " + permissions[i]);
                        }
                    }
                }
                sendResponse(grantedPermissions.equals(mPermissionsToAsk));
                break;
        }
    }

    public void sendResponse(boolean allPermissionGranted) {
        PermissionsManager.getInstance().responseReceived(allPermissionGranted);
        finish();
    }
}
