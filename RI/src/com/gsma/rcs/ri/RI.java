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

package com.gsma.rcs.ri;

import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsListActivity;
import com.gsma.rcs.ri.capabilities.TestCapabilitiesApi;
import com.gsma.rcs.ri.contacts.TestContactsApi;
import com.gsma.rcs.ri.extension.TestMultimediaSessionApi;
import com.gsma.rcs.ri.intents.TestIntentApps;
import com.gsma.rcs.ri.messaging.TestMessagingApi;
import com.gsma.rcs.ri.service.TestServiceApi;
import com.gsma.rcs.ri.sharing.TestSharingApi;
import com.gsma.rcs.ri.upload.InitiateFileUpload;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactUtil;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * RI application
 * 
 * @author Jean-Marc AUFFRET
 */
public class RI extends RcsListActivity {

    private static final int PROGRESS_INIT_INCREMENT = 100;

    private static final String LOGTAG = LogUtils.getTag(RI.class.getSimpleName());

    public static String sChatIdOnForeground;

    private ListView mListView;

    private ProgressBar mProgressBar;

    private LinearLayout mInitLayout;

    /**
     * List of permissions needed for service Just need to ask one permission per dangerous group
     */
    private static final Set<String> sAllPermissionsList = new HashSet<>(Arrays.asList(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.CAMERA,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO));

    private static final int MY_PERMISSION_REQUEST_ALL = 5428;
    private Set<String> mPermissionsToAsk;
    private ListView mListViewStartActivity;
    private View mViewStartActivity;
    private int mPositionStartActivity;
    private long mIdStartActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.ri_list);

        mListView = (ListView) findViewById(android.R.id.list);
        mProgressBar = (ProgressBar) findViewById(android.R.id.progress);
        mInitLayout = (LinearLayout) findViewById(R.id.wait_cnx_start);

        /* Set items */
        // @formatter:off
        String[] items = {
                getString(R.string.menu_contacts),
                getString(R.string.menu_capabilities),
                getString(R.string.menu_messaging),
                getString(R.string.menu_sharing),
                getString(R.string.menu_mm_session),
                getString(R.string.menu_intents),
                getString(R.string.menu_service),
                getString(R.string.menu_upload),
                getString(R.string.menu_about)
        };
        // @formatter:on
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));

        ContactUtil contactUtil = ContactUtil.getInstance(this);
        try {
            /* The country code must be read to check that ContactUtil is ready to be used */
            String cc = contactUtil.getMyCountryCode();
            if (LogUtils.isActive) {
                Log.w(LOGTAG, "Country code is '" + cc + "'");
            }

        } catch (RcsPermissionDeniedException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            /* We should not be allowed to continue if this exception occurs */
            showMessageThenExit(R.string.error_api_permission_denied);
        }
        /*
         * The initialization of the connection manager is delayed to avoid non response during
         * application initialization after installation. The application waits until end of
         * connection manager initialization.
         */
        if (!RiApplication.sCnxManagerStarted) {
            new WaitForConnectionManagerStart()
                    .execute(RiApplication.DELAY_FOR_STARTING_CNX_MANAGER);
        } else {
            mInitLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mListViewStartActivity = l;
            mViewStartActivity = v;
            mPositionStartActivity = position;
            mIdStartActivity = id;
            askPermissions();
        } else {
            startActivityFromItemClick(l, v, position, id);
        }
    }

    private void startActivityFromItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                startActivity(new Intent(this, TestContactsApi.class));
                break;

            case 1:
                startActivity(new Intent(this, TestCapabilitiesApi.class));
                break;

            case 2:
                startActivity(new Intent(this, TestMessagingApi.class));
                break;

            case 3:
                startActivity(new Intent(this, TestSharingApi.class));
                break;

            case 4:
                startActivity(new Intent(this, TestMultimediaSessionApi.class));
                break;

            case 5:
                startActivity(new Intent(this, TestIntentApps.class));
                break;

            case 6:
                startActivity(new Intent(this, TestServiceApi.class));
                break;

            case 7:
                startActivity(new Intent(this, InitiateFileUpload.class));
                break;

            case 8:
                startActivity(new Intent(this, AboutRI.class));
                break;
        }
    }

    private class WaitForConnectionManagerStart extends AsyncTask<Long, Integer, Void> {

        @Override
        protected void onPreExecute() {
            mInitLayout.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgressBar.setProgress(progress[0]);
        }

        @Override
        protected Void doInBackground(Long... duration) {
            long delay = (duration[0] / PROGRESS_INIT_INCREMENT);
            for (int i = 0; i < PROGRESS_INIT_INCREMENT; i++) {
                try {
                    Thread.sleep(delay);
                    publishProgress((int) (delay * (i + 1) * 100 / duration[0]));
                    if (RiApplication.sCnxManagerStarted) {
                        break;
                    }
                } catch (InterruptedException ignore) {
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            mInitLayout.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }

    }

    /**
     * Main function to ask permissions
     */
    private void askPermissions() {
        mPermissionsToAsk = getNotGrantedPermissions();
        if (mPermissionsToAsk.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(mPermissionsToAsk.toArray(new String[mPermissionsToAsk.size()]),
                    MY_PERMISSION_REQUEST_ALL);
        } else {
            startActivityFromItemClick(mListViewStartActivity, mViewStartActivity,
                    mPositionStartActivity, mIdStartActivity);
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
                        Log.w(LOGTAG, "Permission Denied: " + permissions[i]);
                    }
                }
                if (grantedPermissions.equals(mPermissionsToAsk)) {
                    startActivityFromItemClick(mListViewStartActivity, mViewStartActivity,
                            mPositionStartActivity, mIdStartActivity);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RI.this);
                    builder.setMessage(getString(R.string.LabelPermissionsError)).setTitle(
                            getString(R.string.LabelTitlePermissionsError));
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                break;
        }
    }

}
