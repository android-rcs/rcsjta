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
import com.gsma.services.rcs.ipcall.IIPCallListener;
import com.gsma.services.rcs.ipcall.IPCallIntent;

import android.content.Intent;
import android.os.RemoteCallbackList;

/**
 * IPCallEventBroadcaster maintains the registering and unregistering of IIPCallListener and also
 * performs broadcast events on these listeners upon the trigger of corresponding callbacks.
 */
public class IPCallEventBroadcaster implements IIPCallEventBroadcaster {

    private final RemoteCallbackList<IIPCallListener> mIpCallListeners = new RemoteCallbackList<IIPCallListener>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public IPCallEventBroadcaster() {
    }

    public void addEventListener(IIPCallListener listener) {
        mIpCallListeners.register(listener);
    }

    public void removeEventListener(IIPCallListener listener) {
        mIpCallListeners.unregister(listener);
    }

    public void broadcastIPCallStateChanged(ContactId contact, String callId, int state,
            int reasonCode) {
        final int N = mIpCallListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mIpCallListeners.getBroadcastItem(i).onIPCallStateChanged(contact, callId, state,
                        reasonCode);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mIpCallListeners.finishBroadcast();
    }

    public void broadcastIPCallInvitation(String callId) {
        Intent invitation = new Intent(IPCallIntent.ACTION_NEW_INVITATION);
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(invitation);
        IntentUtils.tryToSetReceiverForegroundFlag(invitation);
        invitation.putExtra(IPCallIntent.EXTRA_CALL_ID, callId);
        AndroidFactory.getApplicationContext().sendBroadcast(invitation);
    }
}
