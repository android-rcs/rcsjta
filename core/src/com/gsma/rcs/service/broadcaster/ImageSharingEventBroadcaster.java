/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.service.broadcaster;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.IImageSharingListener;
import com.gsma.services.rcs.sharing.image.ImageSharing.ReasonCode;
import com.gsma.services.rcs.sharing.image.ImageSharing.State;
import com.gsma.services.rcs.sharing.image.ImageSharingIntent;

import android.content.Intent;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ImageSharingEventBroadcaster maintains the registering and unregistering of IImageSharingListener
 * and also performs broadcast events on these listeners upon the trigger of corresponding
 * callbacks.
 */
public class ImageSharingEventBroadcaster implements IImageSharingEventBroadcaster {

    private final RemoteCallbackList<IImageSharingListener> mImageSharingListeners = new RemoteCallbackList<>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public ImageSharingEventBroadcaster() {
    }

    public void addEventListener(IImageSharingListener listener) {
        mImageSharingListeners.register(listener);
    }

    public void removeEventListener(IImageSharingListener listener) {
        mImageSharingListeners.unregister(listener);
    }

    @Override
    public void broadcastStateChanged(ContactId contact, String sharingId, State state,
            ReasonCode reasonCode) {
        int rcsState = state.toInt();
        int rcsReasonCode = reasonCode.toInt();
        final int N = mImageSharingListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mImageSharingListeners.getBroadcastItem(i).onStateChanged(contact, sharingId,
                        rcsState, rcsReasonCode);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mImageSharingListeners.finishBroadcast();
    }

    @Override
    public void broadcastProgressUpdate(ContactId contact, String sharingId, long currentSize,
            long totalSize) {
        final int N = mImageSharingListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mImageSharingListeners.getBroadcastItem(i).onProgressUpdate(contact, sharingId,
                        currentSize, totalSize);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mImageSharingListeners.finishBroadcast();
    }

    @Override
    public void broadcastInvitation(String sharingId) {
        Intent invitation = new Intent(ImageSharingIntent.ACTION_NEW_INVITATION);
        invitation.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(invitation);
        invitation.putExtra(ImageSharingIntent.EXTRA_SHARING_ID, sharingId);
        AndroidFactory.getApplicationContext().sendBroadcast(invitation);
    }

    @Override
    public void broadcastDeleted(ContactId contact, Set<String> sharingIds) {
        List<String> ids = new ArrayList<>(sharingIds);
        final int N = mImageSharingListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mImageSharingListeners.getBroadcastItem(i).onDeleted(contact, ids);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mImageSharingListeners.finishBroadcast();
    }
}
