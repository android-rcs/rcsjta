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
package com.orangelabs.rcs.ri.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.session.MultimediaSession;
import org.gsma.joyn.session.MultimediaSessionService;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List of multimedia sessions in progress
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSessionList extends ListActivity implements JoynServiceListener {
	/**
	 * MM session API
	 */
	private MultimediaSessionService sessionApi;

	/**
	 * List of sessions
	 */
	private List<MultimediaSession> sessions = new ArrayList<MultimediaSession>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.session_list);

        // Set title
        setTitle(R.string.menu_mm_sessions_list);

        // Instanciate API
        sessionApi = new MultimediaSessionService(getApplicationContext(), this);
        
        // Connect API
        sessionApi.connect();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

        // Disconnect API
        sessionApi.disconnect();
	}	
	
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
    	// Display the list of sessions
		updateList();
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
    	Utils.showMessageAndExit(MultimediaSessionList.this, getString(R.string.label_api_disabled));
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
			Intent intent = new Intent(this, MultimediaSessionView.class);
			String sessionId = sessions.get(position).getSessionId();
			intent.putExtra(MultimediaSessionView.EXTRA_MODE, MultimediaSessionView.MODE_OPEN);
			intent.putExtra(MultimediaSessionView.EXTRA_SESSION_ID, sessionId);
			startActivity(intent);
		} catch(JoynServiceException e) {
			Utils.showMessageAndExit(MultimediaSessionList.this, getString(R.string.label_api_failed));
		}
	}

    /**
     * Update the displayed list
     */
    private void updateList() {
		try {
	    	// Reset the list
			sessions.clear();
	    	
	    	// Get list of pending sessions
	    	Set<MultimediaSession> currentSessions = sessionApi.getSessions(TestMultimediaSessionApi.SERVICE_ID);
	    	sessions = new ArrayList<MultimediaSession>(currentSessions);
			if (sessions.size() > 0){
		        String[] items = new String[sessions.size()];    
		        for (int i = 0; i < items.length; i++) {
					items[i] = getString(R.string.label_session, sessions.get(i).getSessionId());
		        }
				setListAdapter(new ArrayAdapter<String>(MultimediaSessionList.this, android.R.layout.simple_list_item_1, items));
			} else {
				setListAdapter(null);
			}
		} catch(Exception e) {
			Utils.showMessageAndExit(MultimediaSessionList.this, getString(R.string.label_api_failed));
		}
    }
}