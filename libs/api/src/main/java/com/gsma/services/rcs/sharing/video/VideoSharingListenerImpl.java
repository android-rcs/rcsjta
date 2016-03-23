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

package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoSharing.ReasonCode;
import com.gsma.services.rcs.sharing.video.VideoSharing.State;

import android.os.RemoteException;
import android.util.Log;

import java.util.HashSet;
import java.util.List;

/**
 * Video sharing event listener implementation
 * 
 * @hide
 */
public class VideoSharingListenerImpl extends IVideoSharingListener.Stub {

    private final VideoSharingListener mListener;

    private final static String LOG_TAG = VideoSharingListenerImpl.class.getName();

    VideoSharingListenerImpl(VideoSharingListener listener) {
        mListener = listener;
    }

    public void onStateChanged(ContactId contact, String sharingId, int state, int reasonCode) {
        State rcsStatus;
        ReasonCode rcsReasonCode;
        try {
            rcsStatus = State.valueOf(state);
            rcsReasonCode = ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can of course not handle since it is build only to handle the
             * possible enum values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }
        mListener.onStateChanged(contact, sharingId, rcsStatus, rcsReasonCode);
    }

    /**
     * This feature to be implemented in CR005
     */
    @Override
    public void onDeleted(ContactId contact, List<String> sharingIds) throws RemoteException {
        mListener.onDeleted(contact, new HashSet<>(sharingIds));
    }
}
