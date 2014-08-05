/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.orangelabs.rcs.service.broadcaster;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.fsh.IFileSharingListener;
import com.orangelabs.rcs.utils.logger.Logger;

import android.os.RemoteCallbackList;

/**
 * FileSharingEventBroadcaster maintains the registering and unregistering of
 * IFileSharingListener and also performs broadcast events on these listeners upon the
 * trigger of corresponding callbacks.
 */
public class FileSharingEventBroadcaster implements IFileSharingEventBroadcaster {

	private final RemoteCallbackList<IFileSharingListener> mFileSharingListeners = new RemoteCallbackList<IFileSharingListener>();

	private final Logger logger = Logger.getLogger(getClass().getName());

	public FileSharingEventBroadcaster() {
	}

	public void addEventListener(IFileSharingListener listener) {
		mFileSharingListeners.register(listener);
	}

	public void removeEventListener(IFileSharingListener listener) {
		mFileSharingListeners.unregister(listener);
	}

	public void broadcastFileSharingStateChanged(ContactId contact, String sharingId, int state) {
		final int N = mFileSharingListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				// TODO : Handle reason code in CR009
				mFileSharingListeners.getBroadcastItem(i).onFileSharingStateChanged(contact,
						sharingId, state);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mFileSharingListeners.finishBroadcast();
	}

	public void broadcastFileSharingProgress(ContactId contact, String sharingId, long currentSize,
			long totalSize) {
		final int N = mFileSharingListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mFileSharingListeners.getBroadcastItem(i).onFileSharingProgress(contact,
						sharingId, currentSize, totalSize);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mFileSharingListeners.finishBroadcast();
	}
}
