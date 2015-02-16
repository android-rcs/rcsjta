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

package com.orangelabs.rcs.ri.ipcall;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.ipcall.IPCall;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List of current IP calls
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallSessionsList extends ListActivity {

    /**
     * List of calls
     */
    private List<IPCall> calls = new ArrayList<IPCall>();

    /**
     * API connection manager
     */
    private ConnectionManager connectionManager;

    /**
     * A locker to exit only once
     */
    private LockAccess exitOnce = new LockAccess();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.ipcall_list);

        // Set title
        setTitle(R.string.menu_ipcall_list);

        // Register to API connection manager
        connectionManager = ConnectionManager.getInstance(this);
        if (connectionManager == null
                || !connectionManager.isServiceConnected(RcsServiceName.IP_CALL)) {
            Utils.showMessageAndExit(this, getString(R.string.label_service_not_available),
                    exitOnce);
            return;
        }
        connectionManager.startMonitorServices(this, null, RcsServiceName.IP_CALL);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update the list of sessions
        updateList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionManager != null) {
            connectionManager.stopMonitorServices(this);
        }
    }

    /**
     * Callback called when service is registered to the RCS/IMS platform
     */
    public void onServiceRegistered() {
        // Nothing to do here
    }

    /**
     * Callback called when service is unregistered from the RCS/IMS platform
     */
    public void onServiceUnregistered() {
        // Update the list of sessions
        updateList();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // Display the selected session
        try {
            Intent intent = new Intent(this, IPCallView.class);
            String callId = calls.get(position).getCallId();
            intent.putExtra(IPCallView.EXTRA_MODE, IPCallView.MODE_OPEN);
            intent.putExtra(IPCallView.EXTRA_CALL_ID, callId);
            startActivity(intent);
        } catch (RcsServiceException e) {
            Utils.showMessageAndExit(IPCallSessionsList.this, getString(R.string.label_api_failed),
                    exitOnce, e);
        }
    }

    /**
     * Update the displayed list
     */
    private void updateList() {
        try {
            // Reset the list
            calls.clear();

            // Get list of pending sessions
            Set<IPCall> currentCalls = connectionManager.getIPCallApi().getIPCalls();
            calls = new ArrayList<IPCall>(currentCalls);
            if (calls.size() > 0) {
                String[] items = new String[calls.size()];
                for (int i = 0; i < items.length; i++) {
                    items[i] = getString(R.string.label_session, calls.get(i).getCallId());
                }
                setListAdapter(new ArrayAdapter<String>(IPCallSessionsList.this,
                        android.R.layout.simple_list_item_1, items));
            } else {
                setListAdapter(null);
            }
        } catch (Exception e) {
            Utils.showMessageAndExit(IPCallSessionsList.this, getString(R.string.label_api_failed),
                    exitOnce, e);
        }
    }
}
