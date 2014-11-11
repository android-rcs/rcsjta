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

package com.orangelabs.rcs.core.ims.service.ipcall;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.content.AudioContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.provider.ipcall.IPCallHistory;

import android.net.Uri;

/**
 * IPCallPersistedStorageAccessor helps in retrieving persisted data related to
 * a IP call from the persisted storage. It can utilize caching for such data
 * that will not be changed after creation of the IP call to speed up consecutive
 * access.
 */
public class IPCallPersistedStorageAccessor {

	private final String mCallId;

	private final IPCallHistory mIPCallLog;

	private ContactId mContact;

	/**
	 * TODO: Change type to enum in CR031 implementation
	 */
	private Integer mDirection;

	public IPCallPersistedStorageAccessor(String callId, IPCallHistory ipCallLog) {
		mCallId = callId;
		mIPCallLog = ipCallLog;
	}

	public ContactId getRemoteContact() {
		/*
		 * Utilizing cache here as contact can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mContact == null) {
			mContact = mIPCallLog.getRemoteContact(mCallId);
		}
		return mContact;
	}

	public int getState() {
		return mIPCallLog.getState(mCallId);
	}

	public int getReasonCode() {
		return mIPCallLog.getReasonCode(mCallId);
	}

	public int getDirection() {
		/*
		 * Utilizing cache here as direction can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mDirection == null) {
			mDirection = mIPCallLog.getDirection(mCallId);
		}
		return mDirection;
	}

	public void setStateAndReasonCode(int state, int reasonCode) {
		mIPCallLog.setCallStateAndReasonCode(mCallId, state, reasonCode);
	}

	public Uri addCall(ContactId contact, int direction, AudioContent audiocontent,
			VideoContent videocontent, int state, int reasonCode) {
		return mIPCallLog.addCall(mCallId, contact, direction, audiocontent, videocontent, state,
				reasonCode);
	}
}
