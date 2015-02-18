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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.gsma.services.rcs.GroupDeliveryInfo;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.GroupFileTransferListener;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.chat.SendFile;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Send file to group
 * 
 * @author jexa7410
 * @author Philippe LEMORDANT
 */
public class SendGroupFile extends SendFile {
    /**
     * Intent parameters
     */
    private final static String EXTRA_CHAT_ID = "chat_id";

    /**
     * Chat ID
     */
    private String mChatId;

    /**
     * The log tag for this class
     */
    private static final String LOGTAG = LogUtils.getTag(SendGroupFile.class.getSimpleName());

    /**
     * File transfer listener
     */
    private GroupFileTransferListener ftListener = new GroupFileTransferListener() {

        @Override
        public void onDeliveryInfoChanged(String chatId, ContactId contact, String transferId,
                GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onSingleRecipientDeliveryStateChanged chatId=" + chatId
                        + " contact=" + contact + " trasnferId=" + transferId + " state=" + status
                        + " reason=" + reasonCode);
            }
        }

        @Override
        public void onProgressUpdate(String chatId, String transferId, final long currentSize,
                final long totalSize) {
            // Discard event if not for current transferId
            if (mTransferId == null || !mTransferId.equals(transferId)) {
                return;
            }
            handler.post(new Runnable() {
                public void run() {
                    // Display transfer progress
                    updateProgressBar(currentSize, totalSize);
                }
            });
        }

        @Override
        public void onStateChanged(String chatId, String transferId,
                final FileTransfer.State state,
                FileTransfer.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onTransferStateChanged chatId=" + chatId + " transferId="
                        + transferId + " state=" + state + " reason=" + reasonCode);
            }

            // Discard event if not for current transferId
            if (mTransferId == null || !mTransferId.equals(transferId)) {
                return;
            }
            final String _reasonCode = RiApplication.FT_REASON_CODES[reasonCode.toInt()];
            final String _state = RiApplication.FT_STATES[state.toInt()];
            handler.post(new Runnable() {
                public void run() {
                    TextView statusView = (TextView) findViewById(R.id.progress_status);
                    switch (state) {
                        case STARTED:
                        case TRANSFERRED:
                            // hide progress dialog
                            hideProgressDialog();
                            // Display transfer state started
                            statusView.setText(_state);
                            break;

                        case ABORTED:
                            // Transfer is aborted: hide progress dialog then
                            // exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(SendGroupFile.this,
                                    getString(R.string.label_transfer_aborted, _reasonCode),
                                    mExitOnce);
                            break;

                        case REJECTED:
                            // Transfer is rejected: hide progress dialog then
                            // exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(SendGroupFile.this,
                                    getString(R.string.label_transfer_rejected, _reasonCode),
                                    mExitOnce);
                            break;

                        case FAILED:
                            // Transfer failed: hide progress dialog then exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(SendGroupFile.this,
                                    getString(R.string.label_transfer_failed, _reasonCode),
                                    mExitOnce);
                            break;

                        default:
                            statusView.setText(_state);
                    }
                }
            });
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get chat ID
        mChatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
    }

    /**
     * Start SendGroupFile activity
     * 
     * @param context
     * @param chatId
     */
    public static void startActivity(Context context, String chatId) {
        Intent intent = new Intent(context, SendGroupFile.class);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        context.startActivity(intent);
    }

    @Override
    public boolean transferFile(Uri file, boolean fileicon) {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "initiateTransfer filename=" + filename + " size=" + filesize
                        + " chatId=" + mChatId);
            }
            // Initiate transfer
            fileTransfer = mCnxManager.getFileTransferApi().transferFileToGroupChat(mChatId, file,
                    fileicon);
            mTransferId = fileTransfer.getTransferId();
            return true;
        } catch (Exception e) {
            hideProgressDialog();
            Utils.showMessageAndExit(this, getString(R.string.label_invitation_failed), mExitOnce,
                    e);
            return false;
        }
    }

    @Override
    public void addFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException {
        fileTransferService.addEventListener(ftListener);
    }

    @Override
    public void removeFileTransferEventListener(FileTransferService fileTransferService)
            throws RcsServiceException {
        fileTransferService.removeEventListener(ftListener);
    }

}
