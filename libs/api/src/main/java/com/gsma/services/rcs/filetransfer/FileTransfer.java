/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.services.rcs.filetransfer;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsUnsupportedOperationException;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.util.SparseArray;

/**
 * File transfer
 *
 * @author Jean-Marc AUFFRET
 */
public class FileTransfer {

    /**
     * File disposition
     */
    public enum Disposition {
        /**
         * Attachment
         */
        ATTACH(0),

        /**
         * Render
         */
        RENDER(1);

        private final int mValue;

        private static SparseArray<Disposition> mValueToEnum = new SparseArray<Disposition>();
        static {
            for (Disposition entry : Disposition.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private Disposition(int value) {
            mValue = value;
        }

        /**
         * Returns the value of this Disposition as an integer.
         *
         * @return integer value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a Disposition instance representing the specified integer value.
         *
         * @param value the integer value
         * @return State instance
         */
        public final static Disposition valueOf(int value) {
            Disposition entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(State.class.getName()).append("").append(value).append("!").toString());
        }
    }

    /**
     * File transfer state
     */
    public enum State {
        /**
         * File transfer invitation received
         */
        INVITED(0),

        /**
         * File transfer has been accepted and is in the process of becoming started
         */
        ACCEPTING(1),

        /**
         * File transfer is rejected
         */
        REJECTED(2),

        /**
         * File transfer has been queued
         */
        QUEUED(3),

        /**
         * File transfer initiating
         */
        INITIATING(4),

        /**
         * File transfer is started
         */
        STARTED(5),

        /**
         * File transfer is paused
         */
        PAUSED(6),

        /**
         * File transfer has been aborted
         */
        ABORTED(7),

        /**
         * File transfer has been transferred with success
         */
        TRANSFERRED(8),

        /**
         * File transfer has failed
         */
        FAILED(9),

        /**
         * File transfer has been delivered
         */
        DELIVERED(10),

        /**
         * File transfer has been displayed or opened
         */
        DISPLAYED(11);

        private final int mValue;

        private static SparseArray<State> mValueToEnum = new SparseArray<State>();
        static {
            for (State entry : State.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private State(int value) {
            mValue = value;
        }

        /**
         * Returns the value of this State as an integer.
         *
         * @return integer value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a State instance representing the specified integer value.
         *
         * @param value the integer value
         * @return State instance
         */
        public final static State valueOf(int value) {
            State entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(State.class.getName()).append("").append(value).append("!").toString());
        }

    }

    /**
     * File transfer reason code
     */
    public enum ReasonCode {
        /**
         * No specific reason code specified.
         */
        UNSPECIFIED(0),

        /**
         * File transfer is aborted by local user.
         */
        ABORTED_BY_USER(1),

        /**
         * File transfer is aborted by remote user..
         */
        ABORTED_BY_REMOTE(2),

        /**
         * File transfer is aborted by system.
         */
        ABORTED_BY_SYSTEM(3),

        /**
         * file transfer is rejected because already taken by the secondary device.
         */
        REJECTED_BY_SECONDARY_DEVICE(4),

        /**
         * File transfer has been rejected by timeout.
         */
        REJECTED_BY_TIMEOUT(5),

        /**
         * Incoming file transfer was rejected as it was detected as spam.
         */
        REJECTED_SPAM(6),

        /**
         * Incoming file transfer was rejected as is cannot be received due to lack of local storage
         * space.
         */
        REJECTED_LOW_SPACE(7),

        /**
         * Incoming transfer was rejected as it was too big to be received.
         */
        REJECTED_MAX_SIZE(8),

        /**
         * Incoming file transfer was rejected as there was too many file transfers ongoing.
         */
        REJECTED_MAX_FILE_TRANSFERS(9),

        /**
         * File transfer invitation was rejected by local user.
         */
        REJECTED_BY_USER(10),

        /**
         * File transfer invitation was rejected by remote.
         */
        REJECTED_BY_REMOTE(11),

        /**
         * File transfer invitation was rejected because of media failure
         */
        REJECTED_MEDIA_FAILED(12),

        /**
         * File transfer invitation was rejected by system.
         */
        REJECTED_BY_SYSTEM(13),

        /**
         * File transfer was paused by system.
         */
        PAUSED_BY_SYSTEM(14),

        /**
         * File transfer was paused by user.
         */
        PAUSED_BY_USER(15),

        /**
         * File transfer initiation failed.
         */
        FAILED_INITIATION(16),

        /**
         * The transferring of the file contents (data) from/to remote side failed.
         */
        FAILED_DATA_TRANSFER(17),

        /**
         * Saving of the incoming file transfer failed.
         */
        FAILED_SAVING(18),

        /**
         * Delivering of the file transfer invitation failed.
         */
        FAILED_DELIVERY(19),

        /**
         * Displaying of the file transfer invitation failed.
         */
        FAILED_DISPLAY(20),

        /**
         * File transfer not allowed to be sent.
         */
        FAILED_NOT_ALLOWED_TO_SEND(21);

        private final int mValue;

        private static SparseArray<ReasonCode> mValueToEnum = new SparseArray<ReasonCode>();
        static {
            for (ReasonCode entry : ReasonCode.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private ReasonCode(int value) {
            mValue = value;
        }

        /**
         * Returns the value of this ReasonCode as an integer.
         *
         * @return integer value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a ReasonCode instance representing the specified integer value.
         *
         * @param value the integer value
         * @return ReasonCode instance
         */
        public final static ReasonCode valueOf(int value) {
            ReasonCode entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(ReasonCode.class.getName()).append("").append(value).append("!")
                    .toString());
        }
    }

    /**
     * File transfer interface
     */
    private final IFileTransfer mTransferInf;

    /**
     * Constructor
     *
     * @param transferIntf File transfer interface
     * @hide
     */
    /* package private */FileTransfer(IFileTransfer transferIntf) {
        mTransferInf = transferIntf;
    }

    /**
     * Returns the chat ID if this file transfer is a group file transfer
     *
     * @return String Chat ID
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getChatId() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getChatId();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the file transfer ID of the file transfer
     *
     * @return String Transfer ID
     * @throws RcsGenericException
     */
    public String getTransferId() throws RcsGenericException {
        try {
            return mTransferInf.getTransferId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the remote contact identifier
     *
     * @return ContactId
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ContactId getRemoteContact() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getRemoteContact();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the complete filename including the path of the file to be transferred
     *
     * @return String Filename
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getFileName() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getFileName();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the size of the file to be transferred
     *
     * @return long Size in bytes
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getFileSize() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getFileSize();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the MIME type of the file to be transferred
     *
     * @return String Type
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getMimeType() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getMimeType();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the Uri of the file icon
     *
     * @return Uri the Uri of the file icon or thumbnail
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public Uri getFileIcon() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getFileIcon();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the MIME type of the file icon to be transfered
     *
     * @return String MIME Type
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public String getFileIconMimeType() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getFileIconMimeType();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the Uri of the file
     *
     * @return Uri of file
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public Uri getFile() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getFile();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the state of the file transfer
     *
     * @return State
     * @see FileTransfer.State
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public State getState() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return State.valueOf(mTransferInf.getState());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the reason code of the state of the sharing
     *
     * @return ReasonCode
     * @see ReasonCode
     * @see FileTransfer.ReasonCode
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public ReasonCode getReasonCode() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return ReasonCode.valueOf(mTransferInf.getReasonCode());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the direction of the transfer
     *
     * @return Direction
     * @see Direction
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public Direction getDirection() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return Direction.valueOf(mTransferInf.getDirection());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the file transfer was initiated and/or queued for
     * outgoing file transfers or the local timestamp of when the file transfer invitation was
     * received for incoming file transfers
     *
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestamp() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getTimestamp();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the file transfer was initiated and /or queued for
     * outgoing file transfers or the remote timestamp of when the file transfer was initiated for
     * incoming file transfers
     *
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestampSent() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getTimestampSent();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the file transfer was delivered for outgoing file
     * transfers or 0 for incoming file transfers or it was not yet displayed
     *
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestampDelivered() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getTimestampDelivered();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the file transfer was displayed for outgoing file
     * transfers or 0 for incoming file transfers or it was not yet displayed
     *
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestampDisplayed() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getTimestampDisplayed();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Accepts file transfer invitation
     *
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void acceptInvitation() throws RcsPermissionDeniedException,
            RcsPersistentStorageException, RcsGenericException {
        try {
            mTransferInf.acceptInvitation();
        } catch (Exception e) {
            RcsUnsupportedOperationException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Rejects file transfer invitation
     *
     * @throws RcsPersistentStorageException
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void rejectInvitation() throws RcsPersistentStorageException,
            RcsPermissionDeniedException, RcsGenericException {
        try {
            mTransferInf.rejectInvitation();
        } catch (Exception e) {
            RcsUnsupportedOperationException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Aborts the file transfer
     *
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void abortTransfer() throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mTransferInf.abortTransfer();
        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to pause this file transfer right now, else returns false. If
     * this filetransfer corresponds to a file transfer that is no longer present in the persistent
     * storage false will be returned (this is no error)
     *
     * @return boolean
     * @throws RcsGenericException
     */
    public boolean isAllowedToPauseTransfer() throws RcsGenericException {
        try {
            return mTransferInf.isAllowedToPauseTransfer();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Pauses the file transfer
     *
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void pauseTransfer() throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mTransferInf.pauseTransfer();
        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            RcsUnsupportedOperationException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to resume this file transfer right now, else return false. If
     * this filetransfer corresponds to a file transfer that is no longer present in the persistent
     * storage false will be returned.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToResumeTransfer() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mTransferInf.isAllowedToResumeTransfer();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Resumes the file transfer
     *
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void resumeTransfer() throws RcsPermissionDeniedException,
            RcsPersistentStorageException, RcsGenericException {
        try {
            mTransferInf.resumeTransfer();
        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns whether you can resend the transfer.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isAllowedToResendTransfer() throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            return mTransferInf.isAllowedToResendTransfer();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Resend a file transfer which was previously failed. This only for 1-1 file transfer, an
     * exception is thrown in case of a file transfer to group.
     *
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void resendTransfer() throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mTransferInf.resendTransfer();
        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            RcsUnsupportedOperationException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if file transfer has been marked as read
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isRead() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.isRead();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the time for when file on the content server is no longer valid to download.
     *
     * @return long time in milliseconds or 0 if not applicable or -1 if unknown
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getFileExpiration() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getFileExpiration();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the time for when file icon on the content server is no longer valid to download.
     *
     * @return long time in milliseconds or 0 if not applicable or -1 if unknown
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getFileIconExpiration() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.getFileIconExpiration();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if delivery for this file has expired or false otherwise. Note: false means
     * either that delivery for this file has not yet expired, delivery has been successful,
     * delivery expiration has been cleared (see clearFileTransferDeliveryExpiration) or that this
     * particular file is not eligible for delivery expiration in the first place.
     *
     * @return boolean
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public boolean isExpiredDelivery() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mTransferInf.isExpiredDelivery();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
