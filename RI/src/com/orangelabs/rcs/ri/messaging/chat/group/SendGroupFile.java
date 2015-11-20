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

package com.orangelabs.rcs.ri.messaging.chat.group;

import static com.orangelabs.rcs.ri.utils.FileUtils.takePersistableContentUriPermission;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.GroupFileTransferListener;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.chat.SendFile;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.Set;

/**
 * Send file to group
 * 
 * @author jexa7410
 * @author Philippe LEMORDANT
 */
public class SendGroupFile extends SendFile {

    private final static String EXTRA_CHAT_ID = "chat_id";

    private String mChatId;

    private static final String LOGTAG = LogUtils.getTag(SendGroupFile.class.getSimpleName());

    private GroupFileTransferListener mFileTransferListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
    }

    /**
     * Start SendGroupFile activity
     * 
     * @param context The context
     * @param chatId The chat ID
     */
    public static void startActivity(Context context, String chatId) {
        Intent intent = new Intent(context, SendGroupFile.class);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        context.startActivity(intent);
    }

    @Override
    public boolean transferFile(Uri file, boolean fileIcon) {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "initiateTransfer filename=" + mFilename + " size=" + mFilesize
                        + " chatId=" + mChatId);
            }
            /* Only take persistable permission for content Uris */
            takePersistableContentUriPermission(this, file);
            /* Initiate transfer */
            mFileTransfer = mFileTransferService.transferFileToGroupChat(mChatId, file, fileIcon);
            mTransferId = mFileTransfer.getTransferId();
            return true;

        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
            return false;
        }
    }

    @Override
    public void addFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException {
        fileTransferService.addEventListener(mFileTransferListener);
    }

    @Override
    public void removeFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException {
        fileTransferService.removeEventListener(mFileTransferListener);
    }

    @Override
    public void initialize() {
        super.initialize();
        mFileTransferListener = new GroupFileTransferListener() {

            @Override
            public void onDeliveryInfoChanged(String chatId, ContactId contact, String transferId,
                    GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onDeliveryInfoChanged chatId=" + chatId + " contact=" + contact
                            + " trasnferId=" + transferId + " state=" + status + " reason="
                            + reasonCode);
                }
            }

            @Override
            public void onProgressUpdate(String chatId, String transferId, final long currentSize,
                    final long totalSize) {
                /* Discard event if not for current transferId */
                if (mTransferId == null || !mTransferId.equals(transferId)) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        updateProgressBar(currentSize, totalSize);
                    }
                });
            }

            @Override
            public void onStateChanged(String chatId, String transferId,
                    final FileTransfer.State state, final FileTransfer.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged chatId=" + chatId + " transferId=" + transferId
                            + " state=" + state + " reason=" + reasonCode);
                }
                /* Discard event if not for current transferId */
                if (mTransferId == null || !mTransferId.equals(transferId)) {
                    return;
                }
                final String _reasonCode = RiApplication.sFileTransferReasonCodes[reasonCode
                        .toInt()];
                final String _state = RiApplication.sFileTransferStates[state.toInt()];
                mHandler.post(new Runnable() {

                    public void run() {
                        if (mFileTransfer != null) {
                            try {
                                mResumeBtn.setEnabled(mFileTransfer.isAllowedToResumeTransfer());

                            } catch (RcsServiceException e) {
                                mResumeBtn.setEnabled(false);
                                showException(e);
                            }
                            try {
                                mPauseBtn.setEnabled(mFileTransfer.isAllowedToPauseTransfer());

                            } catch (RcsServiceException e) {
                                mPauseBtn.setEnabled(false);
                                showException(e);
                            }
                        }
                        TextView statusView = (TextView) findViewById(R.id.progress_status);
                        switch (state) {
                            case STARTED:
                                //$FALL-THROUGH$
                            case TRANSFERRED:
                                hideProgressDialog();
                                /* Display transfer state started */
                                statusView.setText(_state);
                                break;

                            case ABORTED:
                                showMessageThenExit(getString(R.string.label_transfer_aborted,
                                        _reasonCode));
                                break;

                            case REJECTED:
                                showMessageThenExit(getString(R.string.label_transfer_rejected,
                                        _reasonCode));
                                break;

                            case FAILED:
                                showMessageThenExit(getString(R.string.label_transfer_failed,
                                        _reasonCode));
                                break;

                            default:
                                statusView.setText(_state);
                        }
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
