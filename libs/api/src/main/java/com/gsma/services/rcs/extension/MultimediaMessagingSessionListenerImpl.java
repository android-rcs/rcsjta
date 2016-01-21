/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaSession.ReasonCode;
import com.gsma.services.rcs.extension.MultimediaSession.State;

import android.util.Log;

/**
 * Multimedia Messaging Session Listener Implementation
 * 
 * @hide
 */
public class MultimediaMessagingSessionListenerImpl extends
        IMultimediaMessagingSessionListener.Stub {

    private final MultimediaMessagingSessionListener mListener;

    private static final String LOG_TAG = MultimediaMessagingSessionListenerImpl.class.getName();

    MultimediaMessagingSessionListenerImpl(MultimediaMessagingSessionListener listener) {
        mListener = listener;
    }

    public void onStateChanged(ContactId contact, String sessionId, int state, int reasonCode) {
        State rcsState;
        ReasonCode rcsReasonCode;
        try {
            rcsState = State.valueOf(state);
            rcsReasonCode = ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can not handle since it is built only to handle the possible enum
             * values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }
        mListener.onStateChanged(contact, sessionId, rcsState, rcsReasonCode);
    }

    public void onMessageReceived(ContactId contact, String sessionId, byte[] content) {
        mListener.onMessageReceived(contact, sessionId, content);
    }

    public void onMessageReceived2(ContactId contact, String sessionId, byte[] content, String contentType) {
        mListener.onMessageReceived(contact, sessionId, content, contentType);
    }

    public void onMessagesFlushed(ContactId contact, String sessionId) {
        mListener.onMessagesFlushed(contact, sessionId);
    }
}
