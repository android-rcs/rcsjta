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

package com.gsma.rcs.service.broadcaster;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.extension.IMultimediaStreamingSessionListener;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;

import android.content.Intent;
import android.os.RemoteCallbackList;

/**
 * MultimediaStreamingSessionEventBroadcaster maintains the registering and unregistering of
 * IMultimediaStreamingSessionListener and also performs broadcast events on these listeners upon
 * the trigger of corresponding callbacks.
 */
public class MultimediaStreamingSessionEventBroadcaster implements
        IMultimediaStreamingSessionEventBroadcaster {

    private final RemoteCallbackList<IMultimediaStreamingSessionListener> mMultimediaStreamingListeners = new RemoteCallbackList<IMultimediaStreamingSessionListener>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public MultimediaStreamingSessionEventBroadcaster() {
    }

    public void addMultimediaStreamingEventListener(IMultimediaStreamingSessionListener listener) {
        mMultimediaStreamingListeners.register(listener);
    }

    public void removeMultimediaStreamingEventListener(IMultimediaStreamingSessionListener listener) {
        mMultimediaStreamingListeners.unregister(listener);
    }

    public void broadcastPayloadReceived(ContactId contact, String sessionId, byte[] content) {
        final int N = mMultimediaStreamingListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mMultimediaStreamingListeners.getBroadcastItem(i).onPayloadReceived(contact,
                        sessionId, content);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mMultimediaStreamingListeners.finishBroadcast();
    }

    public void broadcastStateChanged(ContactId contact, String sessionId, int state, int reasonCode) {
        final int N = mMultimediaStreamingListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mMultimediaStreamingListeners.getBroadcastItem(i).onStateChanged(contact,
                        sessionId, state, reasonCode);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mMultimediaStreamingListeners.finishBroadcast();
    }

    public void broadcastInvitation(String sessionId, Intent rtpSessionInvite) {
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(rtpSessionInvite);
        IntentUtils.tryToSetReceiverForegroundFlag(rtpSessionInvite);
        rtpSessionInvite.putExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID, sessionId);

        AndroidFactory.getApplicationContext().sendBroadcast(rtpSessionInvite);
    }
}
