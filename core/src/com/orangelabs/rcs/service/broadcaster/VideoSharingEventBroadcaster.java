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
import com.gsma.services.rcs.vsh.IVideoSharingListener;
import com.orangelabs.rcs.service.api.VideoSharingServiceImpl;
import com.orangelabs.rcs.utils.logger.Logger;

import android.os.RemoteCallbackList;

/**
 * VideoSharingEventBroadcaster maintains the registering and unregistering of
 * IVideoSharingListener and also performs broadcast events on these listeners upon the
 * trigger of corresponding callbacks.
 */
public class VideoSharingEventBroadcaster implements IVideoSharingEventBroadcaster {

	private final RemoteCallbackList<IVideoSharingListener> mVideoSharingListeners = new RemoteCallbackList<IVideoSharingListener>();

	private final Logger logger = Logger.getLogger(getClass().getName());

	public VideoSharingEventBroadcaster() {
	}

	public void addEventListener(IVideoSharingListener listener) {
		mVideoSharingListeners.register(listener);
	}

	public void removeEventListener(IVideoSharingListener listener) {
		mVideoSharingListeners.unregister(listener);
	}

	public void broadcastVideoSharingStateChanged(ContactId contact, String sharingId, int state) {
		final int N = mVideoSharingListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				// TODO : Handle reason code in CR009
				mVideoSharingListeners.getBroadcastItem(i).onVideoSharingStateChanged(contact,
						sharingId, state);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mVideoSharingListeners.finishBroadcast();
	}
}
