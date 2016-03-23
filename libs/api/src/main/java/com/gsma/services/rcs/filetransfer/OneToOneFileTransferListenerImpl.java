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

package com.gsma.services.rcs.filetransfer;

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.os.RemoteException;
import android.util.Log;

import java.util.HashSet;
import java.util.List;

/**
 * File transfer event listener
 * 
 * @hide
 */
public class OneToOneFileTransferListenerImpl extends IOneToOneFileTransferListener.Stub {

    private final OneToOneFileTransferListener mListener;

    private final static String LOG_TAG = OneToOneFileTransferListenerImpl.class.getName();

    OneToOneFileTransferListenerImpl(OneToOneFileTransferListener listener) {
        mListener = listener;
    }

    public void onStateChanged(ContactId contact, String transferId, int state, int reasonCode) {
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
        mListener.onStateChanged(contact, transferId, rcsState, rcsReasonCode);
    }

    public void onProgressUpdate(ContactId contact, String transferId, long currentSize,
            long totalSize) {
        mListener.onProgressUpdate(contact, transferId, currentSize, totalSize);
    }

    /**
     * This feature to be implemented in CR005
     */
    @Override
    public void onDeleted(ContactId contact, List<String> transferIds) throws RemoteException {
        mListener.onDeleted(contact, new HashSet<>(transferIds));
    }
}
