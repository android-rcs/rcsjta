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
package com.orangelabs.rcs.ri.extension.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.widget.ArrayAdapter;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.extension.MultimediaSessionList;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List of messaging sessions in progress
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessagingSessionList extends MultimediaSessionList {
	/**
	 * List of sessions
	 */
	private List<MultimediaMessagingSession> sessions = new ArrayList<MultimediaMessagingSession>();
	
	/**
	 * Display a session
	 * 
	 * @param position
	 */
	public void displaySession(int position) {
		try {
			Intent intent = new Intent(this, MessagingSessionView.class);
			String sessionId = sessions.get(position).getSessionId();
			intent.putExtra(MessagingSessionView.EXTRA_MODE, MessagingSessionView.MODE_OPEN);
			intent.putExtra(MessagingSessionView.EXTRA_SESSION_ID, sessionId);
			startActivity(intent);
		} catch(RcsServiceException e) {
			e.printStackTrace();
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed));
		}
	}
	
    /**
     * Update the displayed list
     */
    public void updateList() {
		try {
			// Reset the list
			sessions.clear();

			// Get list of pending sessions
			Set<MultimediaMessagingSession> currentSessions = connectionManager.getMultimediaSessionApi().getMessagingSessions(
					MessagingSessionUtils.SERVICE_ID);
			sessions = new ArrayList<MultimediaMessagingSession>(currentSessions);
			if (sessions.size() > 0) {
				String[] items = new String[sessions.size()];
				for (int i = 0; i < items.length; i++) {
					items[i] = getString(R.string.label_session, sessions.get(i).getSessionId());
				}
				setListAdapter(new ArrayAdapter<String>(MessagingSessionList.this, android.R.layout.simple_list_item_1, items));
			} else {
				setListAdapter(null);
			}
		} catch (Exception e) {
			Utils.showMessageAndExit(MessagingSessionList.this, getString(R.string.label_api_failed), exitOnce);
		}
	}
}