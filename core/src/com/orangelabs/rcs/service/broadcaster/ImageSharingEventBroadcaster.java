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
import com.gsma.services.rcs.ish.IImageSharingListener;
import com.gsma.services.rcs.ish.ImageSharingIntent;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.IntentUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import android.content.Intent;
import android.os.RemoteCallbackList;

/**
 * ImageSharingEventBroadcaster maintains the registering and unregistering of
 * IImageSharingListener and also performs broadcast events on these listeners upon the
 * trigger of corresponding callbacks.
 */
public class ImageSharingEventBroadcaster implements IImageSharingEventBroadcaster {

	private final RemoteCallbackList<IImageSharingListener> mImageSharingListeners = new RemoteCallbackList<IImageSharingListener>();

	private final Logger logger = Logger.getLogger(getClass().getName());

	public ImageSharingEventBroadcaster() {
	}

	public void addEventListener(IImageSharingListener listener) {
		mImageSharingListeners.register(listener);
	}

	public void removeEventListener(IImageSharingListener listener) {
		mImageSharingListeners.unregister(listener);
	}

	public void broadcastStateChanged(ContactId contact, String sharingId, int state,
			int reasonCode) {
		final int N = mImageSharingListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mImageSharingListeners.getBroadcastItem(i).onStateChanged(contact,
						sharingId, state, reasonCode);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mImageSharingListeners.finishBroadcast();
	}

	public void broadcastProgressUpdate(ContactId contact, String sharingId, long currentSize,
			long totalSize) {
		final int N = mImageSharingListeners.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mImageSharingListeners.getBroadcastItem(i).onProgressUpdate(contact,
						sharingId, currentSize, totalSize);
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Can't notify listener", e);
				}
			}
		}
		mImageSharingListeners.finishBroadcast();
	}

	public void broadcastInvitation(String sharingId) {
		Intent invitation = new Intent(ImageSharingIntent.ACTION_NEW_INVITATION);
		IntentUtils.tryToSetExcludeStoppedPackagesFlag(invitation);
		IntentUtils.tryToSetReceiverForegroundFlag(invitation);
		invitation.putExtra(ImageSharingIntent.EXTRA_SHARING_ID, sharingId);
		AndroidFactory.getApplicationContext().sendBroadcast(invitation);
	}
}
