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

package com.orangelabs.rcs.ri.extension;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.ListActivity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

/**
 * Abstract list of multimedia sessions
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class MultimediaSessionList extends ListActivity {

    /**
     * API connection manager
     */
    protected ConnectionManager mCnxManager;

    /**
     * A locker to exit only once
     */
    protected LockAccess mExitOnce = new LockAccess();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.extension_session_list);

        // Register to API connection manager
        mCnxManager = ConnectionManager.getInstance();
        if (!mCnxManager.isServiceConnected(RcsServiceName.MULTIMEDIA)) {
            Utils.showMessageAndExit(MultimediaSessionList.this,
                    getString(R.string.label_service_not_available), mExitOnce);
            return;
        }
        mCnxManager.startMonitorServices(this, null, RcsServiceName.MULTIMEDIA);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mExitOnce.isLocked()) {
            // Update the list of sessions
            updateList();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // Display the selected session
        displaySession(position);
    }

    /**
     * Display a session
     * 
     * @param position
     */
    public abstract void displaySession(int position);

    /**
     * Update the displayed list
     */
    public abstract void updateList();
}
