/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri.messaging.filetransfer.multi;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.GroupFileTransferListener;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.util.Log;

import java.util.List;
import java.util.Set;

/**
 * SendMultiFileGroupChat
 * 
 * @author Philippe LEMORDANT
 */
public class SendMultiFileGroupChat extends SendMultiFile implements ISendMultiFile {

    private static final String LOGTAG = LogUtils.getTag(SendMultiFileGroupChat.class
            .getSimpleName());

    private GroupFileTransferListener mFtListener;

    @Override
    public boolean transferFiles(List<FileTransferProperties> filesToTransfer) {
        try {
            for (FileTransferProperties fileToTransfer : filesToTransfer) {
                /* Initiate transfer */
                FileTransfer fileTransfer = mFileTransferService.transferFileToGroupChat(mChatId,
                        fileToTransfer.getUri(), fileToTransfer.isFileicon());
                String fileTransferId = fileTransfer.getTransferId();
                mTransferIds.add(fileTransferId);
                mFileTransfers.add(fileTransfer);
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Transfer file '" + fileToTransfer.getFilename() + "' to chatId="
                            + mChatId + " icon=" + fileToTransfer.isFileicon() + " fileTransferId="
                            + fileTransferId);
                }
            }
            return true;
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return false;
        }
    }

    @Override
    public void addFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException {
        if (!mFileTransferListenerAdded) {
            fileTransferService.addEventListener(mFtListener);
            mFileTransferListenerAdded = true;
        }
    }

    @Override
    public void removeFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException {
        if (mFileTransferListenerAdded) {
            fileTransferService.removeEventListener(mFtListener);
            mFileTransferListenerAdded = false;
        }
    }

    @Override
    public boolean checkPermissionToSendFile(String chatId) throws RcsServiceException {
        return mFileTransferService.isAllowedToTransferFileToGroupChat(chatId);
    }

    @Override
    public void initialize() {
        mFtListener = new GroupFileTransferListener() {

            @Override
            public void onDeliveryInfoChanged(String chatId, ContactId contact, String transferId,
                    GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onDeliveryInfoChanged chatId=" + chatId + " contact=" + contact
                            + " transferId=" + transferId + " state=" + status + " reason="
                            + reasonCode);
                }
            }

            @Override
            public void onProgressUpdate(String chatId, final String transferId,
                    final long currentSize, final long totalSize) {
                /* Discard event if not for current transferId */
                if (!mTransferIds.contains(transferId)) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        updateProgressBar(mTransferIds.indexOf(transferId), currentSize, totalSize);
                    }
                });
            }

            @Override
            public void onStateChanged(String chatId, final String transferId,
                    final FileTransfer.State state, FileTransfer.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged chatId=" + chatId + " transferId=" + transferId
                            + " state=" + state + " reason=" + reasonCode);
                }
                /* Discard event if not for current transferId */
                if (!mTransferIds.contains(transferId)) {
                    return;
                }

                final String _reasonCode;
                if (FileTransfer.ReasonCode.UNSPECIFIED == reasonCode) {
                    _reasonCode = null;
                } else {
                    _reasonCode = RiApplication.sFileTransferReasonCodes[reasonCode.toInt()];
                }
                final String _state = RiApplication.sFileTransferStates[state.toInt()];
                final FileTransferProperties prop = mFileTransferAdapter.getItem(mTransferIds
                        .indexOf(transferId));
                mHandler.post(new Runnable() {
                    public void run() {
                        prop.setStatus(_state);
                        prop.setReasonCode(_reasonCode);
                        mFileTransferAdapter.notifyDataSetChanged();
                        closeDialogIfMultipleFileTransferIsFinished();
                    }
                });
            }

            @Override
            public void onDeleted(String chatId, Set<String> transferIds) {
                if (LogUtils.isActive) {
                    Log.w(LOGTAG, "onDeleted chatId=" + chatId + " transferIds=" + transferIds);
                }
            }

        };
    }

}
