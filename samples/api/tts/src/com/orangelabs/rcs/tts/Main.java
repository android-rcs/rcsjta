/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.orangelabs.rcs.tts;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Text-To-Speech for incoming chat and group chat invitation
 * 
 * @author jexa7410
 */
public class Main extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private CheckBoxPreference activateCheck;
    /**
     * List of permissions needed for service Just need to ask one permission per dangerous group
     */
    private static final Set<String> sAllPermissionsList = new HashSet<>(
            Arrays.asList(Manifest.permission.READ_CONTACTS));

    private static final int MY_PERMISSION_REQUEST_ALL = 5428;
    private Set<String> mPermissionsToAsk;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        askPermissions();
        // Set title
        setTitle(R.string.app_title);

        // Set preferences
        addPreferencesFromResource(R.xml.tts_preferences);
        activateCheck = (CheckBoxPreference) getPreferenceScreen().findPreference("activate");
        activateCheck.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load preferences
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(
                Registry.REGISTRY, Activity.MODE_PRIVATE);
        boolean flag = Registry.readBoolean(preferences, Registry.ACTIVATE_TTS, false);
        activateCheck.setChecked(flag);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        // Update preferences
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(
                Registry.REGISTRY, Activity.MODE_PRIVATE);
        if (preference.getKey().equals("activate")) {
            boolean flag = !activateCheck.isChecked();
            Registry.writeBoolean(preferences, Registry.ACTIVATE_TTS, flag);
        }
        return true;
    }

    /**
     * Main function to ask permissions
     */
    private void askPermissions() {
        mPermissionsToAsk = getNotGrantedPermissions();
        if (mPermissionsToAsk.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(mPermissionsToAsk.toArray(new String[mPermissionsToAsk.size()]),
                    MY_PERMISSION_REQUEST_ALL);
        }
    }

    /**
     * Check all permissions's status
     * 
     * @return Set of permissions that are not granted
     */
    private Set<String> getNotGrantedPermissions() {
        Set<String> permissionsToAsk = new HashSet<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return permissionsToAsk;
        }
        for (String permission : sAllPermissionsList) {
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(permission)) {
                permissionsToAsk.add(permission);
            }
        }
        return permissionsToAsk;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_ALL:
                Set<String> grantedPermissions = new HashSet<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        grantedPermissions.add(permissions[i]);
                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Log.w("Permissions", "Permission Denied: " + permissions[i]);
                    }
                }
                if (!grantedPermissions.equals(mPermissionsToAsk)) {
                    finish();
                }
                break;
        }
    }
}
