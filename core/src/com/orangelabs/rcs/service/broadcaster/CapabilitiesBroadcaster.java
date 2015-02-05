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

import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.ICapabilitiesListener;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.utils.logger.Logger;

import android.os.RemoteCallbackList;

import java.util.HashMap;

/**
 * CapabilitiesBroadcaster maintains the registering and unregistering of ICapabilitiesListener and
 * also performs broadcast events on these listeners upon the trigger of corresponding callbacks.
 */
public class CapabilitiesBroadcaster implements ICapabilitiesBroadcaster {

    private RemoteCallbackList<ICapabilitiesListener> mCapabilitiesListeners = new RemoteCallbackList<ICapabilitiesListener>();

    private HashMap<ContactId, RemoteCallbackList<ICapabilitiesListener>> mCapalitiesListenersPerContact = new HashMap<ContactId, RemoteCallbackList<ICapabilitiesListener>>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public CapabilitiesBroadcaster() {
    }

    private void broadcastCapabilitiesReceivedForAllContacts(ContactId contact,
            Capabilities contactCapabilities) {
        final int N = mCapabilitiesListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mCapabilitiesListeners.getBroadcastItem(i).onCapabilitiesReceived(contact,
                        contactCapabilities);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mCapabilitiesListeners.finishBroadcast();
    }

    private void broadcastCapabilitiesReceivedOnPerContact(ContactId contact,
            Capabilities contactCapabilities) {
        RemoteCallbackList<ICapabilitiesListener> capabilitiesListeners = mCapalitiesListenersPerContact
                .get(contact);
        if (capabilitiesListeners == null) {
            return;
        }
        final int N = capabilitiesListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                capabilitiesListeners.getBroadcastItem(i).onCapabilitiesReceived(contact,
                        contactCapabilities);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        capabilitiesListeners.finishBroadcast();
    }

    public void addCapabilitiesListener(ICapabilitiesListener listener) {
        mCapabilitiesListeners.register(listener);
    }

    public void removeCapabilitiesListener(ICapabilitiesListener listener) {
        mCapabilitiesListeners.unregister(listener);
    }

    public void addContactCapabilitiesListener(ContactId contact, ICapabilitiesListener listener) {
        RemoteCallbackList<ICapabilitiesListener> capabilitiesListeners = mCapalitiesListenersPerContact
                .get(contact);
        if (capabilitiesListeners == null) {
            capabilitiesListeners = new RemoteCallbackList<ICapabilitiesListener>();
            mCapalitiesListenersPerContact.put(contact, capabilitiesListeners);
        }
        capabilitiesListeners.register(listener);
    }

    public void removeContactCapabilitiesListener(ContactId contact, ICapabilitiesListener listener) {
        RemoteCallbackList<ICapabilitiesListener> listeners = mCapalitiesListenersPerContact
                .get(contact);
        if (listeners != null) {
            listeners.unregister(listener);
        }
    }

    public void broadcastCapabilitiesReceived(ContactId contact, Capabilities contactCapabilities) {
        // Notify capabilities listeners
        broadcastCapabilitiesReceivedForAllContacts(contact, contactCapabilities);
        // Notify capabilities listeners for a given contact
        broadcastCapabilitiesReceivedOnPerContact(contact, contactCapabilities);
    }
}
