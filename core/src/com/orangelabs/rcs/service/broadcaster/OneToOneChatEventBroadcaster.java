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

import android.os.RemoteCallbackList;

import com.gsma.services.rcs.chat.IChatListener;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * OneToOneChatEventBroadcaster maintains the registering and unregistering of
 * IChatListeners and also performs broadcast events on these listeners upon the
 * trigger of corresponding callbacks.
 */
public class OneToOneChatEventBroadcaster implements IOneToOneChatEventBroadcaster {

	private final RemoteCallbackList<IChatListener> mOneToOneChatListeners = new RemoteCallbackList<IChatListener>();

	private final Logger logger = Logger.getLogger(getClass().getName());

	public OneToOneChatEventBroadcaster() {
	}

	public void addOneToOneChatEventListener(IChatListener listener) {
		mOneToOneChatListeners.register(listener);
	}

	public void removeOneToOneChatEventListener(IChatListener listener) {
		mOneToOneChatListeners.unregister(listener);
	}

	public void broadcastMessageStatusChanged(ContactId contact, String msgId, int status) {
		final int N = mOneToOneChatListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				// TODO : Handle reason code in CR009
				mOneToOneChatListeners.getBroadcastItem(i).onMessageStatusChanged(contact, msgId,
						status);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mOneToOneChatListeners.finishBroadcast();
	}

	public void broadcastComposingEvent(ContactId contact, boolean status) {
		final int N = mOneToOneChatListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mOneToOneChatListeners.getBroadcastItem(i).onComposingEvent(contact, status);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mOneToOneChatListeners.finishBroadcast();
	}
}
