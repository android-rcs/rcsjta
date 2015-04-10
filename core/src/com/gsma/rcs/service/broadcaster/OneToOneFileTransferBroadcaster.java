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
import com.gsma.rcs.service.api.ServerApiException;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.FileTransferIntent;
import com.gsma.services.rcs.filetransfer.IOneToOneFileTransferListener;

import android.content.Intent;
import android.os.RemoteCallbackList;

import java.util.List;

/**
 * OneToOneFileTransferBroadcaster maintains the registering and unregistering of
 * IFileTransferListener and also performs broadcast events on these listeners upon the trigger of
 * corresponding callbacks.
 */
public class OneToOneFileTransferBroadcaster implements IOneToOneFileTransferBroadcaster {

    private final RemoteCallbackList<IOneToOneFileTransferListener> mOneToOneFileTransferListeners = new RemoteCallbackList<IOneToOneFileTransferListener>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public OneToOneFileTransferBroadcaster() {
    }

    public void addOneToOneFileTransferListener(IOneToOneFileTransferListener listener)
            throws ServerApiException {
        mOneToOneFileTransferListeners.register(listener);
    }

    public void removeOneToOneFileTransferListener(IOneToOneFileTransferListener listener)
            throws ServerApiException {
        mOneToOneFileTransferListeners.unregister(listener);
    }

    public void broadcastStateChanged(ContactId contact, String transferId, State state,
            ReasonCode reasonCode) {
        final int N = mOneToOneFileTransferListeners.beginBroadcast();
        int rcsState = state.toInt();
        int rcsReasonCode = reasonCode.toInt();
        for (int i = 0; i < N; i++) {
            try {
                mOneToOneFileTransferListeners.getBroadcastItem(i).onStateChanged(contact,
                        transferId, rcsState, rcsReasonCode);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mOneToOneFileTransferListeners.finishBroadcast();
    }

    public void broadcastProgressUpdate(ContactId contact, String transferId, long currentSize,
            long totalSize) {
        final int N = mOneToOneFileTransferListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mOneToOneFileTransferListeners.getBroadcastItem(i).onProgressUpdate(contact,
                        transferId, currentSize, totalSize);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mOneToOneFileTransferListeners.finishBroadcast();
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

    public void broadcastFileTransferDeleted(ContactId contact, List<String> filetransferIds) {
        final int N = mOneToOneFileTransferListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mOneToOneFileTransferListeners.getBroadcastItem(i).onDeleted(contact,
                        filetransferIds);
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mOneToOneFileTransferListeners.finishBroadcast();
    }
}
