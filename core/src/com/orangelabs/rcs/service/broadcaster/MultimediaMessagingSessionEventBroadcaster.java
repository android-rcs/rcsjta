/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.orangelabs.rcs.service.broadcaster;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.extension.IMultimediaMessagingSessionListener;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.IntentUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import android.content.Intent;
import android.os.RemoteCallbackList;

/**
 * MultimediaMessagingSessionEventBroadcaster maintains the registering and
 * unregistering of IMultimediaMessagingSessionListener and also performs
 * broadcast events on these listeners upon the trigger of corresponding
 * callbacks.
 */
public class MultimediaMessagingSessionEventBroadcaster implements
		IMultimediaMessagingSessionEventBroadcaster {

	private final RemoteCallbackList<IMultimediaMessagingSessionListener> mMultimediaMessagingListeners = new RemoteCallbackList<IMultimediaMessagingSessionListener>();

	private final Logger logger = Logger.getLogger(getClass().getName());

	public MultimediaMessagingSessionEventBroadcaster() {
	}

	public void addMultimediaMessagingEventListener(IMultimediaMessagingSessionListener listener) {
		mMultimediaMessagingListeners.register(listener);
	}

	public void removeMultimediaMessagingEventListener(IMultimediaMessagingSessionListener listener) {
		mMultimediaMessagingListeners.unregister(listener);
	}

	public void broadcastMessageReceived(ContactId contact, String sessionId, byte[] message) {
		final int N = mMultimediaMessagingListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mMultimediaMessagingListeners.getBroadcastItem(i).onMessageReceived(contact, sessionId,
						message);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mMultimediaMessagingListeners.finishBroadcast();
	}

	public void broadcastStateChanged(ContactId contact, String sessionId,
			int state, int reasonCode) {
		final int N = mMultimediaMessagingListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mMultimediaMessagingListeners.getBroadcastItem(i)
						.onStateChanged(contact, sessionId, state, reasonCode);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mMultimediaMessagingListeners.finishBroadcast();
	}

	public void broadcastInvitation(String sessionId, Intent msrpSessionInvite) {
		IntentUtils.tryToSetExcludeStoppedPackagesFlag(msrpSessionInvite);
		IntentUtils.tryToSetReceiverForegroundFlag(msrpSessionInvite);
		msrpSessionInvite.putExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID, sessionId);

		AndroidFactory.getApplicationContext().sendBroadcast(msrpSessionInvite);
	}
}
