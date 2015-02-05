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

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.IVideoSharingListener;
import com.gsma.services.rcs.vsh.VideoSharingIntent;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.IntentUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * VideoSharingEventBroadcaster maintains the registering and unregistering of IVideoSharingListener
 * and also performs broadcast events on these listeners upon the trigger of corresponding
 * callbacks.
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

    public void broadcastStateChanged(ContactId contact, String sharingId, int state, int reasonCode) {
        final int N = mVideoSharingListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mVideoSharingListeners.getBroadcastItem(i).onStateChanged(contact, sharingId,
                        state, reasonCode);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mVideoSharingListeners.finishBroadcast();
    }

    public void broadcastInvitation(String sharingId) {
        Intent newInvitation = new Intent(VideoSharingIntent.ACTION_NEW_INVITATION);
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(newInvitation);
        IntentUtils.tryToSetReceiverForegroundFlag(newInvitation);
        newInvitation.putExtra(VideoSharingIntent.EXTRA_SHARING_ID, sharingId);
        AndroidFactory.getApplicationContext().sendBroadcast(newInvitation);
    }
}
