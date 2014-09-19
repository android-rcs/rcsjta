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
import com.gsma.services.rcs.ft.IFileTransferListener;
import com.orangelabs.rcs.service.api.ServerApiException;
import com.orangelabs.rcs.utils.logger.Logger;

import android.os.RemoteCallbackList;

/**
 * OneToOneFileTransferBroadcaster maintains the registering and unregistering of
 * IFileTransferListener and also performs broadcast events on these listeners upon
 * the trigger of corresponding callbacks.
 */
public class OneToOneFileTransferBroadcaster implements IOneToOneFileTransferBroadcaster {

	private final RemoteCallbackList<IFileTransferListener> mOneToOneFileTransferListeners = new RemoteCallbackList<IFileTransferListener>();

	private final Logger logger = Logger.getLogger(getClass().getName());

	public OneToOneFileTransferBroadcaster() {
	}

	public void addOneToOneFileTransferListener(IFileTransferListener listener)
			throws ServerApiException {
		mOneToOneFileTransferListeners.register(listener);
	}

	public void removeOneToOneFileTransferListener(IFileTransferListener listener)
			throws ServerApiException {
		mOneToOneFileTransferListeners.unregister(listener);
	}

	public void broadcastTransferStateChanged(ContactId contact, String transferId, int state, int reasonCode) {
		final int N = mOneToOneFileTransferListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mOneToOneFileTransferListeners.getBroadcastItem(i).onTransferStateChanged(contact,
						transferId, state, reasonCode);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mOneToOneFileTransferListeners.finishBroadcast();
	}

	public void broadcastTransferprogress(ContactId contact, String transferId, long currentSize,
			long totalSize) {
		final int N = mOneToOneFileTransferListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mOneToOneFileTransferListeners.getBroadcastItem(i).onTransferProgress(contact,
						transferId, currentSize, totalSize);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mOneToOneFileTransferListeners.finishBroadcast();
	}
}
