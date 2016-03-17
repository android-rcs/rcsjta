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
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.os.RemoteException;
import android.util.Log;

import java.util.HashSet;
import java.util.List;

/**
 * Group file transfer event listener implementation
 *
 * @hide
 */
public class GroupFileTransferListenerImpl extends IGroupFileTransferListener.Stub {

    private final GroupFileTransferListener mListener;

    private final static String LOG_TAG = GroupFileTransferListenerImpl.class.getName();

    GroupFileTransferListenerImpl(GroupFileTransferListener listener) {
        mListener = listener;
    }

    public void onStateChanged(String chatId, String transferId, int state, int reasonCode) {
        FileTransfer.State rcsState;
        FileTransfer.ReasonCode rcsReasonCode;
        try {
            rcsState = FileTransfer.State.valueOf(state);
            rcsReasonCode = FileTransfer.ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can not handle since it is built only to handle the possible enum
             * values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }
        mListener.onStateChanged(chatId, transferId, rcsState, rcsReasonCode);
    }

    @Override
    public void onProgressUpdate(String chatId, String transferId, long currentSize, long totalSize) {
        mListener.onProgressUpdate(chatId, transferId, currentSize, totalSize);
    }

    public void onDeliveryInfoChanged(String chatId, ContactId contact, String transferId,
            int status, int reasonCode) {
        GroupDeliveryInfo.Status rcsStatus;
        GroupDeliveryInfo.ReasonCode rcsReasonCode;
        try {
            rcsStatus = GroupDeliveryInfo.Status.valueOf(status);
            rcsReasonCode = GroupDeliveryInfo.ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can not handle since it is built only to handle the possible enum
             * values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }

        mListener.onDeliveryInfoChanged(chatId, contact, transferId, rcsStatus, rcsReasonCode);
    }

    /**
     * This feature to be implemented in CR005
     */
    @Override
    public void onDeleted(String chatId, List<String> transferIds) throws RemoteException {
        mListener.onDeleted(chatId, new HashSet<>(transferIds));
    }
}
