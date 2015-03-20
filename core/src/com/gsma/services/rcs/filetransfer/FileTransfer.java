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

import android.net.Uri;
import android.util.SparseArray;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;

/**
 * File transfer
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransfer {

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
                    .append(State.class.getName()).append(".").append(value).append("!").toString());
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
         * File transfer has been rejected by inactivity.
         */
        REJECTED_BY_INACTIVITY(5),

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
         * File transfer was paused by system.
         */
        PAUSED_BY_SYSTEM(13),

        /**
         * File transfer was paused by user.
         */
        PAUSED_BY_USER(14),

        /**
         * File transfer initiation failed.
         */
        FAILED_INITIATION(15),

        /**
         * The transferring of the file contents (data) from/to remote side failed.
         */
        FAILED_DATA_TRANSFER(16),

        /**
         * Saving of the incoming file transfer failed.
         */
        FAILED_SAVING(17),

        /**
         * Delivering of the file transfer invitation failed.
         */
        FAILED_DELIVERY(18),

        /**
         * Displaying of the file transfer invitation failed.
         */
        FAILED_DISPLAY(19),

        /**
         * File transfer not allowed to be sent.
         */
        FAILED_NOT_ALLOWED_TO_SEND(20);

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
                    .append(ReasonCode.class.getName()).append(".").append(value).append("!")
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
     * @return Chat ID
     * @throws RcsServiceException
     */
    public String getChatId() throws RcsServiceException {
        try {
            return mTransferInf.getChatId();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the file transfer ID of the file transfer
     * 
     * @return Transfer ID
     * @throws RcsServiceException
     */
    public String getTransferId() throws RcsServiceException {
        try {
            return mTransferInf.getTransferId();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     * @throws RcsServiceException
     */
    public ContactId getRemoteContact() throws RcsServiceException {
        try {
            return mTransferInf.getRemoteContact();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the complete filename including the path of the file to be transferred
     * 
     * @return Filename
     * @throws RcsServiceException
     */
    public String getFileName() throws RcsServiceException {
        try {
            return mTransferInf.getFileName();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the size of the file to be transferred
     * 
     * @return Size in bytes
     * @throws RcsServiceException
     */
    public long getFileSize() throws RcsServiceException {
        try {
            return mTransferInf.getFileSize();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the MIME type of the file to be transferred
     * 
     * @return Type
     * @throws RcsServiceException
     */
    public String getMimeType() throws RcsServiceException {
        try {
            return mTransferInf.getMimeType();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the Uri of the file icon
     * 
     * @return the Uri of the file icon or thumbnail
     * @throws RcsServiceException
     */
    public Uri getFileIcon() throws RcsServiceException {
        try {
            return mTransferInf.getFileIcon();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the MIME type of the file icon to be transfered
     * 
     * @return MIME Type
     * @throws RcsServiceException
     */
    public String getFileIconMimeType() throws RcsServiceException {
        try {
            return mTransferInf.getFileIconMimeType();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the Uri of the file
     * 
     * @return Uri of file
     * @throws RcsServiceException
     */
    public Uri getFile() throws RcsServiceException {
        try {
            return mTransferInf.getFile();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the state of the file transfer
     * 
     * @return State
     * @see FileTransfer.State
     * @throws RcsServiceException
     */
    public State getState() throws RcsServiceException {
        try {
            return State.valueOf(mTransferInf.getState());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the reason code of the state of the sharing
     * 
     * @return ReasonCode
     * @see ReasonCode
     * @see FileTransfer.ReasonCode
     * @throws RcsServiceException
     */
    public ReasonCode getReasonCode() throws RcsServiceException {
        try {
            return ReasonCode.valueOf(mTransferInf.getReasonCode());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the direction of the transfer
     * 
     * @return Direction
     * @see Direction
     * @throws RcsServiceException
     */
    public Direction getDirection() throws RcsServiceException {
        try {
            return Direction.valueOf(mTransferInf.getDirection());
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns the local timestamp of when the file transfer was initiated and/or queued for
     * outgoing file transfers or the local timestamp of when the file transfer invitation was
     * received for incoming file transfers
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestamp() throws RcsServiceException {
        try {
            return mTransferInf.getTimestamp();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local timestamp of when the file transfer was initiated and /or queued for
     * outgoing file transfers or the remote timestamp of when the file transfer was initiated for
     * incoming file transfers
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestampSent() throws RcsServiceException {
        try {
            return mTransferInf.getTimestampSent();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local timestamp of when the file transfer was delivered for outgoing file
     * transfers or 0 for incoming file transfers or it was not yet displayed
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestampDelivered() throws RcsServiceException {
        try {
            return mTransferInf.getTimestampDelivered();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local timestamp of when the file transfer was displayed for outgoing file
     * transfers or 0 for incoming file transfers or it was not yet displayed
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestampDisplayed() throws RcsServiceException {
        try {
            return mTransferInf.getTimestampDisplayed();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Accepts file transfer invitation
     * 
     * @throws RcsServiceException
     */
    public void acceptInvitation() throws RcsServiceException {
        try {
            mTransferInf.acceptInvitation();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Rejects file transfer invitation
     * 
     * @throws RcsServiceException
     */
    public void rejectInvitation() throws RcsServiceException {
        try {
            mTransferInf.rejectInvitation();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Aborts the file transfer
     * 
     * @throws RcsServiceException
     */
    public void abortTransfer() throws RcsServiceException {
        try {
            mTransferInf.abortTransfer();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns true if it is possible to pause this file transfer right now, else returns false. If
     * this filetransfer corresponds to a file transfer that is no longer present in the persistent
     * storage false will be returned (this is no error)
     * 
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToPauseTransfer() throws RcsServiceException {
        try {
            return mTransferInf.isAllowedToPauseTransfer();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Pauses the file transfer
     * 
     * @throws RcsServiceException
     */
    public void pauseTransfer() throws RcsServiceException {
        try {
            mTransferInf.pauseTransfer();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns true if it is possible to resume this file transfer right now, else return false. If
     * this filetransfer corresponds to a file transfer that is no longer present in the persistent
     * storage false will be returned.
     * 
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToResumeTransfer() throws RcsServiceException {
        try {
            return mTransferInf.isAllowedToResumeTransfer();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Resumes the file transfer
     * 
     * @throws RcsServiceException
     */
    public void resumeTransfer() throws RcsServiceException {
        try {
            mTransferInf.resumeTransfer();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns whether you can resend the transfer.
     * 
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToResendTransfer() throws RcsServiceException {
        try {
            return mTransferInf.isAllowedToResendTransfer();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Resend a file transfer which was previously failed. This only for 1-1 file transfer, an
     * exception is thrown in case of a file transfer to group.
     * 
     * @throws RcsServiceException
     */
    public void resendTransfer() throws RcsServiceException {
        try {
            mTransferInf.resendTransfer();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns true if file transfer has been marked as read
     * 
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isRead() throws RcsServiceException {
        try {
            return mTransferInf.isRead();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns the time for when file on the content server is no longer valid to download.
     * 
     * @return time in milliseconds or 0 if not applicable or -1 if unknown
     * @throws RcsServiceException
     */
    public long getFileExpiration() throws RcsServiceException {
        try {
            return mTransferInf.getFileExpiration();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns the time for when file icon on the content server is no longer valid to download.
     * 
     * @return time in milliseconds or 0 if not applicable or -1 if unknown
     * @throws RcsServiceException
     */
    public long getFileIconExpiration() throws RcsServiceException {
        try {
            return mTransferInf.getFileIconExpiration();
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }
}
