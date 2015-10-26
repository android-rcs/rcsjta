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

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.filetransfer.OneToOneFileTransferListener;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.messaging.chat.SendFile;
import com.orangelabs.rcs.ri.utils.FileUtils;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;

import java.util.Set;

/**
 * Send file to contact
 * 
 * @author jexa7410
 * @author Philippe LEMORDANT
 */
public class SendSingleFile extends SendFile {

    private final static String EXTRA_CONTACT = "contact";

    private ContactId mContact;

    private static final String LOGTAG = LogUtils.getTag(SendSingleFile.class.getSimpleName());

    private OneToOneFileTransferListener mFileTransferListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContact = getIntent().getParcelableExtra(EXTRA_CONTACT);
    }

    /**
     * Start SendFile activity
     * 
     * @param context The context
     * @param contact The contact ID
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
                Log.d(LOGTAG, "initiateTransfer mFilename=" + mFilename + " size=" + mFilesize);
            }
            /* Only take persistable permission for content Uris */
            FileUtils.tryToTakePersistableContentUriPermission(getApplicationContext(), file);
            /* Initiate transfer */
            mFileTransfer = mFileTransferService.transferFile(mContact, file, fileicon);
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
        mFileTransferListener = new OneToOneFileTransferListener() {

            @Override
            public void onProgressUpdate(ContactId contact, String transferId,
                    final long currentSize, final long totalSize) {
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
            public void onStateChanged(ContactId contact, String transferId,
                    final FileTransfer.State state, final FileTransfer.ReasonCode reasonCode) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "onStateChanged contact=" + contact + " transferId=" + transferId
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
                                showExceptionThenExit(e);
                            }
                            try {
                                mPauseBtn.setEnabled(mFileTransfer.isAllowedToPauseTransfer());
                            } catch (RcsServiceException e) {
                                showExceptionThenExit(e);
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
            public void onDeleted(ContactId contact, Set<String> transferIds) {
                if (LogUtils.isActive) {
                    Log.w(LOGTAG, "onDeleted contact=" + contact + " transferIds=" + transferIds);
                }
            }

        };
    }
}
