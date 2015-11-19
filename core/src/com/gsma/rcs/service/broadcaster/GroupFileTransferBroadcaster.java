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
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.IGroupFileTransferListener;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.content.Intent;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * GroupFileTransferBroadcaster maintains the registering and unregistering of
 * IGroupFileTransferListener and also performs broadcast events on these listeners upon the trigger
 * of corresponding callbacks.
 */
public class GroupFileTransferBroadcaster implements IGroupFileTransferBroadcaster {

    private final RemoteCallbackList<IGroupFileTransferListener> mGroupFileTransferListeners = new RemoteCallbackList<>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public GroupFileTransferBroadcaster() {
    }

    public void addGroupFileTransferListener(IGroupFileTransferListener listener) {
        mGroupFileTransferListeners.register(listener);
    }

    public void removeGroupFileTransferListener(IGroupFileTransferListener listener) {
        mGroupFileTransferListeners.unregister(listener);
    }

    public void broadcastStateChanged(String chatId, String transferId, State state,
            ReasonCode reasonCode) {
        final int N = mGroupFileTransferListeners.beginBroadcast();
        int rcsState = state.toInt();
        int rcsReasonCode = reasonCode.toInt();
        for (int i = 0; i < N; i++) {
            try {
                mGroupFileTransferListeners.getBroadcastItem(i).onStateChanged(chatId, transferId,
                        rcsState, rcsReasonCode);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupFileTransferListeners.finishBroadcast();
    }

    public void broadcastProgressUpdate(String chatId, String transferId, long currentSize,
            long totalSize) {
        final int N = mGroupFileTransferListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupFileTransferListeners.getBroadcastItem(i).onProgressUpdate(chatId,
                        transferId, currentSize, totalSize);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupFileTransferListeners.finishBroadcast();
    }

    public void broadcastDeliveryInfoChanged(String chatId, ContactId contact, String transferId,
            GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
        int rcsStatus = status.toInt();
        int rcsReasonCode = reasonCode.toInt();
        final int N = mGroupFileTransferListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupFileTransferListeners.getBroadcastItem(i).onDeliveryInfoChanged(chatId,
                        contact, transferId, rcsStatus, rcsReasonCode);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener per contact", e);
                }
            }
        }
        mGroupFileTransferListeners.finishBroadcast();
    }

    public void broadcastInvitation(String fileTransferId) {
        Intent invitation = new Intent(FileTransferIntent.ACTION_NEW_INVITATION);
        invitation.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(invitation);
        invitation.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, fileTransferId);
        AndroidFactory.getApplicationContext().sendBroadcast(invitation);
    }

    public void broadcastResumeFileTransfer(String filetransferId) {
        Intent resumeFileTransfer = new Intent(FileTransferIntent.ACTION_RESUME);
        resumeFileTransfer.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(resumeFileTransfer);
        resumeFileTransfer.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, filetransferId);
        AndroidFactory.getApplicationContext().sendBroadcast(resumeFileTransfer);
    }

    public void broadcastFileTransfersDeleted(String chatId, Set<String> transferIds) {
        List<String> ids = new ArrayList<>(transferIds);
        final int N = mGroupFileTransferListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupFileTransferListeners.getBroadcastItem(i).onDeleted(chatId, ids);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener per contact", e);
                }
            }
        }
        mGroupFileTransferListeners.finishBroadcast();
    }
}
