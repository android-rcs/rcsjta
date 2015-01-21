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

import android.content.Intent;
import android.os.RemoteCallbackList;

import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.chat.IOneToOneChatListener;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.IntentUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * OneToOneChatEventBroadcaster maintains the registering and unregistering of
 * IOneToOneChatListeners and also performs broadcast events on these listeners upon the
 * trigger of corresponding callbacks.
 */
public class OneToOneChatEventBroadcaster implements IOneToOneChatEventBroadcaster {

	private final RemoteCallbackList<IOneToOneChatListener> mOneToOneChatListeners = new RemoteCallbackList<IOneToOneChatListener>();

	private final Logger logger = Logger.getLogger(getClass().getName());

	public OneToOneChatEventBroadcaster() {
	}

	public void addOneToOneChatEventListener(IOneToOneChatListener listener) {
		mOneToOneChatListeners.register(listener);
	}

	public void removeOneToOneChatEventListener(IOneToOneChatListener listener) {
		mOneToOneChatListeners.unregister(listener);
	}

	public void broadcastMessageStatusChanged(ContactId contact, String mimeType, String msgId,
			int status, int reasonCode) {
		final int N = mOneToOneChatListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mOneToOneChatListeners.getBroadcastItem(i).onMessageStatusChanged(contact,
						mimeType, msgId, status, reasonCode);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener.", e);
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

	public void broadcastMessageReceived(String mimeType, String msgId) {
		Intent newOneToOneMessage = new Intent(OneToOneChatIntent.ACTION_NEW_ONE_TO_ONE_CHAT_MESSAGE);
		IntentUtils.tryToSetExcludeStoppedPackagesFlag(newOneToOneMessage);
		IntentUtils.tryToSetReceiverForegroundFlag(newOneToOneMessage);
		newOneToOneMessage.putExtra(OneToOneChatIntent.EXTRA_MIME_TYPE, mimeType);
		newOneToOneMessage.putExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID, msgId);
		AndroidFactory.getApplicationContext().sendBroadcast(newOneToOneMessage);
	}
}
