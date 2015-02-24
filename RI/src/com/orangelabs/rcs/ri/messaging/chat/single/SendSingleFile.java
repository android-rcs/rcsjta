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

package com.orangelabs.rcs.ri.messaging.chat.single;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;

import java.util.Set;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.chat.SendFile;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Send file to contact
 * 
 * @author jexa7410
 * @author Philippe LEMORDANT
 */
public class SendSingleFile extends SendFile {
    /**
     * Intent parameters
     */
    private final static String EXTRA_CONTACT = "contact";

    private ContactId mContact;

    /**
     * The log tag for this class
     */
    private static final String LOGTAG = LogUtils.getTag(SendSingleFile.class.getSimpleName());

    /**
     * File transfer listener
     */
    private OneToOneFileTransferListener ftListener = new OneToOneFileTransferListener() {

        @Override
        public void onProgressUpdate(ContactId contact, String transferId, final long currentSize,
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
        public void onStateChanged(ContactId contact, String transferId,
                final FileTransfer.State state,
                FileTransfer.ReasonCode reasonCode) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "onTransferStateChanged contact=" + contact + " transferId="
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
                            Utils.showMessageAndExit(SendSingleFile.this,
                                    getString(R.string.label_transfer_aborted, _reasonCode),
                                    mExitOnce);
                            break;

                        case REJECTED:
                            // Transfer is rejected: hide progress dialog then
                            // exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(SendSingleFile.this,
                                    getString(R.string.label_transfer_rejected, _reasonCode),
                                    mExitOnce);
                            break;

                        case FAILED:
                            // Transfer failed: hide progress dialog then exit
                            hideProgressDialog();
                            Utils.showMessageAndExit(SendSingleFile.this,
                                    getString(R.string.label_transfer_failed, _reasonCode),
                                    mExitOnce);
                            break;

                        default:
                            statusView.setText(_state);
                    }
                }
            });
        }

        @Override
        public void onDeleted(ContactId contact, Set<String> transferIds) {
            if (LogUtils.isActive) {
                Log.w(LOGTAG,
                        new StringBuilder("onDeleted contact=").append(contact)
                                .append(" transferIds=")
                                .append(transferIds).toString());
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get contact ID
        mContact = (ContactId) getIntent().getParcelableExtra(EXTRA_CONTACT);
    }

    /**
     * Start SendFile activity
     * 
     * @param context
     * @param contact
     */
    public static void startActivity(Context context, ContactId contact) {
        Intent intent = new Intent(context, SendSingleFile.class);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        context.startActivity(intent);
    }

    @Override
    public boolean transferFile(Uri file, boolean fileicon) {
        try {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "initiateTransfer filename=" + filename + " size=" + filesize);
            }
            // Initiate transfer
            fileTransfer = mCnxManager.getFileTransferApi().transferFile(mContact, file, fileicon);
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
