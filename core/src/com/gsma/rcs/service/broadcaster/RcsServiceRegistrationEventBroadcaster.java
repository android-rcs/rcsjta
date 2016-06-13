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

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * RcsServiceRegistrationEventBroadcaster maintains the registering and unregistering of
 * IRcsServiceRegistrationListener and also performs broadcast events on these listeners upon the
 * trigger of corresponding callbacks.
 */
public class RcsServiceRegistrationEventBroadcaster implements
        IRcsServiceRegistrationEventBroadcaster {

    private final RemoteCallbackList<IRcsServiceRegistrationListener> mServiceRegistrationListeners = new RemoteCallbackList<>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public RcsServiceRegistrationEventBroadcaster() {
    }

    public void addEventListener(IRcsServiceRegistrationListener listener) {
        mServiceRegistrationListeners.register(listener);
    }

    public void removeEventListener(IRcsServiceRegistrationListener listener) {
        mServiceRegistrationListeners.unregister(listener);
    }

    @Override
    public void broadcastServiceRegistered() {
        final int N = mServiceRegistrationListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mServiceRegistrationListeners.getBroadcastItem(i).onServiceRegistered();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mServiceRegistrationListeners.finishBroadcast();
    }

    @Override
    public void broadcastServiceUnRegistered(RcsServiceRegistration.ReasonCode reason) {
        final int N = mServiceRegistrationListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mServiceRegistrationListeners.getBroadcastItem(i).onServiceUnregistered(
                        reason.toInt());
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mServiceRegistrationListeners.finishBroadcast();
    }
}
