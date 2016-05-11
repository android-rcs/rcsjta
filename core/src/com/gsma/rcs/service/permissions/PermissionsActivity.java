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
import com.gsma.rcs.provisioning.local.Provisioning;
import com.gsma.rcs.utils.logger.Logger;

import android.Manifest;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.ArrayAdapter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A class to request permissions for the provisioning tool.
 * 
 * @author Philippe LEMORDANT
 */
public class PermissionsActivity extends ListActivity {

    /**
     * List of permissions needed for service. Just need to ask one permission per dangerous group.
     */
    // @formatter:off
    private static final Set<String> sAllPermissionsList = new HashSet<>(Collections.singletonList(
            Manifest.permission.WRITE_EXTERNAL_STORAGE));
    // @formatter:on

    private static final int MY_PERMISSION_REQUEST_ALL = 5428;

    private static final Logger sLogger = Logger.getLogger(PermissionsActivity.class.getName());

    private static final String PERMISSION_ASKED = "permission_asked";
    private Set<String> notGrantedPermissions;
    private boolean mAskingPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ask_permissions);
        notGrantedPermissions = getNotGrantedPermissions();
        if (savedInstanceState != null) {
            mAskingPermission = savedInstanceState.getBoolean(PERMISSION_ASKED);
            if (!notGrantedPermissions.isEmpty()) {
                if (!mAskingPermission) {
                    mAskingPermission = true;
                    ActivityCompat
                            .requestPermissions(this, notGrantedPermissions
                                    .toArray(new String[notGrantedPermissions.size()]),
                                    MY_PERMISSION_REQUEST_ALL);
                }
                displayUngrantedPermissions();

            } else {
                startActivity(new Intent(this, Provisioning.class));
                finish();
            }
        } else {
            if (notGrantedPermissions.isEmpty()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("All permissions are granted");
                }
                startActivity(new Intent(this, Provisioning.class));
                finish();
            } else {
                if (sLogger.isActivated()) {
                    sLogger.warn("Permission to be asked: " + notGrantedPermissions);
                    askPermissions();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Make sure to call the super method so that the variables of our views are saved
        super.onSaveInstanceState(outState);
        // Save our own variable now
        outState.putBoolean(PERMISSION_ASKED, mAskingPermission);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (MY_PERMISSION_REQUEST_ALL == requestCode) {
            mAskingPermission = false;
            notGrantedPermissions = new HashSet<>();
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    String unGrantedPermission = permissions[i];
                    if (sLogger.isActivated()) {
                        sLogger.warn("Permission Denied: " + unGrantedPermission);
                    }
                    notGrantedPermissions.add(unGrantedPermission);
                }
            }
            if (!notGrantedPermissions.isEmpty()) {
                displayUngrantedPermissions();
            } else {
                startActivity(new Intent(this, Provisioning.class));
                finish();
            }
        }
    }

    private void askPermissions() {
        if (notGrantedPermissions.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // only ask permissions once.
            if (!mAskingPermission) {
                mAskingPermission = true;
                displayUngrantedPermissions();
                requestPermissions(
                        notGrantedPermissions.toArray(new String[notGrantedPermissions.size()]),
                        MY_PERMISSION_REQUEST_ALL);
            }
        } else {
            startActivity(new Intent(this, Provisioning.class));
            finish();
        }
    }

    private Set<String> getNotGrantedPermissions() {
        Set<String> permissionsToAsk = new HashSet<>();
        for (String permission : PermissionsActivity.sAllPermissionsList) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this,
                    permission)) {
                permissionsToAsk.add(permission);
            }
        }
        return permissionsToAsk;
    }

    private void displayUngrantedPermissions() {
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                notGrantedPermissions.toArray(new String[notGrantedPermissions.size()])));
    }

}
