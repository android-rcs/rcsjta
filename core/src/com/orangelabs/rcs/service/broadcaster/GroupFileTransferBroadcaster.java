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

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransferIntent;
import com.gsma.services.rcs.ft.IGroupFileTransferListener;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.service.api.ServerApiException;
import com.orangelabs.rcs.utils.IntentUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import android.content.Intent;
import android.os.RemoteCallbackList;

/**
 * GroupFileTransferBroadcaster maintains the registering and unregistering of
 * IGroupFileTransferListener and also performs broadcast events on these listeners upon the trigger
 * of corresponding callbacks.
 */
public class GroupFileTransferBroadcaster implements IGroupFileTransferBroadcaster {

    private final RemoteCallbackList<IGroupFileTransferListener> mGroupFileTransferListeners = new RemoteCallbackList<IGroupFileTransferListener>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public GroupFileTransferBroadcaster() {
    }

    public void addGroupFileTransferListener(IGroupFileTransferListener listener)
            throws ServerApiException {
        mGroupFileTransferListeners.register(listener);
    }

    public void removeGroupFileTransferListener(IGroupFileTransferListener listener)
            throws ServerApiException {
        mGroupFileTransferListeners.unregister(listener);
    }

    public void broadcastStateChanged(String chatId, String transferId, int state, int reasonCode) {
        final int N = mGroupFileTransferListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupFileTransferListeners.getBroadcastItem(i).onStateChanged(chatId, transferId,
                        state, reasonCode);
            } catch (Exception e) {
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
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupFileTransferListeners.finishBroadcast();
    }

    public void broadcastGroupDeliveryInfoStateChanged(String chatId, ContactId contact,
            String transferId, int state, int reasonCode) {
        final int N = mGroupFileTransferListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupFileTransferListeners.getBroadcastItem(i).onDeliveryInfoChanged(chatId,
                        contact, transferId, state, reasonCode);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener per contact", e);
                }
            }
        }
        mGroupFileTransferListeners.finishBroadcast();
    }

    public void broadcastInvitation(String fileTransferId) {
        Intent invitation = new Intent(FileTransferIntent.ACTION_NEW_INVITATION);
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(invitation);
        IntentUtils.tryToSetReceiverForegroundFlag(invitation);
        invitation.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, fileTransferId);
        AndroidFactory.getApplicationContext().sendBroadcast(invitation);
    }

    public void broadcastResumeFileTransfer(String filetransferId) {
        Intent resumeFileTransfer = new Intent(FileTransferIntent.ACTION_RESUME);
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(resumeFileTransfer);
        IntentUtils.tryToSetReceiverForegroundFlag(resumeFileTransfer);
        resumeFileTransfer.putExtra(FileTransferIntent.EXTRA_TRANSFER_ID, filetransferId);
        AndroidFactory.getApplicationContext().sendBroadcast(resumeFileTransfer);
    }
}
