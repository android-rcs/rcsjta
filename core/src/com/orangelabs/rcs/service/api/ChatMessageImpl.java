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

package com.orangelabs.rcs.service.api;

import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.contacts.ContactId;

public class ChatMessageImpl extends IChatMessage.Stub {

	private final ChatMessagePersistedStorageAccessor mPersistentStorage;

	/**
	 * Constructor
	 * 
	 * @param persistentStorage ChatMessagePersistedStorageAccessor
	 */
	public ChatMessageImpl(ChatMessagePersistedStorageAccessor persistentStorage) {
		mPersistentStorage = persistentStorage;
	}

	public ContactId getContact() {
		return mPersistentStorage.getRemoteContact();
	}

	public String getId() {
		return mPersistentStorage.getId();
	}

	public String getContent() {
		return mPersistentStorage.getContent();
	}

	public String getMimeType() {
		return mPersistentStorage.getMimeType();
	}

	public int getDirection() {
		return mPersistentStorage.getDirection();
	}

	public long getTimestamp() {
		return mPersistentStorage.getTimestamp();
	}

	public long getTimestampSent() {
		return mPersistentStorage.getTimestampSent();
	}

	public long getTimestampDelivered() {
		return mPersistentStorage.getTimestampDelivered();
	}

	public long getTimestampDisplayed() {
		return mPersistentStorage.getTimestampDisplayed();
	}

	public int getStatus() {
		return mPersistentStorage.getStatus();
	}

	public int getReasonCode() {
		return mPersistentStorage.getReasonCode();
	}

	public String getChatId() {
		return mPersistentStorage.getChatId();
	}

	public boolean isRead() {
		return mPersistentStorage.isRead();
	}
}
