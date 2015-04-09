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

package com.gsma.services.rcs.sharing.image;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.util.SparseArray;

/**
 * Image sharing
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharing {

    /**
     * Image sharing state
     */
    public enum State {

        /**
         * Sharing invitation received
         */
        INVITED(0),

        /**
         * Sharing invitation sent
         */
        INITIATING(1),

        /**
         * Sharing is started
         */
        STARTED(2),

        /**
         * Sharing has been aborted
         */
        ABORTED(3),

        /**
         * Sharing has failed
         */
        FAILED(4),

        /**
         * Image has been transferred with success
         */
        TRANSFERRED(5),

        /**
         * Sharing has been rejected
         */
        REJECTED(6),

        /**
         * Ringing
         */
        RINGING(7),

        /**
         * Sharing has been accepted and is in the process of becoming started
         */
        ACCEPTING(8);

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

        public final int toInt() {
            return mValue;
        }

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
     * Reason code associated with the image share state.
     */
    public enum ReasonCode {

        /**
         * No specific reason code specified.
         */
        UNSPECIFIED(0),

        /**
         * Image share is aborted by local user.
         */
        ABORTED_BY_USER(1),

        /**
         * Image share is aborted by remote user.
         */
        ABORTED_BY_REMOTE(2),

        /**
         * Image share is aborted by system.
         */
        ABORTED_BY_SYSTEM(3),

        /**
         * Image share is rejected because already taken by the secondary device.
         */
        REJECTED_BY_SECONDARY_DEVICE(4),

        /**
         * Incoming image was rejected because it is spam.
         */
        REJECTED_SPAM(5),

        /**
         * Incoming image was rejected by timeout.
         */
        REJECTED_BY_TIMEOUT(6),

        /**
         * Incoming image was rejected as is cannot be received due to lack of local storage space.
         */
        REJECTED_LOW_SPACE(7),

        /**
         * Incoming image was rejected as it was too big to be received.
         */
        REJECTED_MAX_SIZE(8),

        /**
         * Incoming image was rejected because max number of sharing sessions is achieved.
         */
        REJECTED_MAX_SHARING_SESSIONS(9),

        /**
         * Incoming image was rejected by local user.
         */
        REJECTED_BY_USER(10),

        /**
         * Incoming image was rejected by remote.
         */
        REJECTED_BY_REMOTE(11),

        /**
         * Image share initiation failed;
         */
        FAILED_INITIATION(12),

        /**
         * Sharing of the image share has failed.
         */
        FAILED_SHARING(13),

        /**
         * Saving of the image share has failed.
         */
        FAILED_SAVING(14);

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

        public final int toInt() {
            return mValue;
        }

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
     * Image sharing interface
     */
    private final IImageSharing mSharingInf;

    /**
     * Constructor
     * 
     * @param sharingInf Image sharing interface
     */
    /* package private */ImageSharing(IImageSharing sharingInf) {
        mSharingInf = sharingInf;
    }

    /**
     * Returns the sharing ID of the image sharing
     * 
     * @return Sharing ID
     * @throws RcsServiceException
     */
    public String getSharingId() throws RcsServiceException {
        try {
            return mSharingInf.getSharingId();
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
            return mSharingInf.getRemoteContact();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the URI of the file to be shared
     * 
     * @return Uri
     * @throws RcsServiceException
     */
    public Uri getFile() throws RcsServiceException {
        try {
            return mSharingInf.getFile();
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
            return mSharingInf.getFileName();
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
            return mSharingInf.getFileSize();
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
            return mSharingInf.getMimeType();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local timestamp of when the image sharing was initiated for outgoing image
     * sharing or the local timestamp of when the image sharing invitation was received for incoming
     * image sharings.
     * 
     * @return long
     * @throws RcsServiceException
     */
    public long getTimestamp() throws RcsServiceException {
        try {
            return mSharingInf.getTimestamp();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the state of the sharing
     * 
     * @return State
     * @see ImageSharing.State
     * @throws RcsServiceException
     */
    public State getState() throws RcsServiceException {
        try {
            return State.valueOf(mSharingInf.getState());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the reason code of the state of the sharing
     * 
     * @return ReasonCode
     * @see ImageSharing.ReasonCode
     * @throws RcsServiceException
     */
    public ReasonCode getReasonCode() throws RcsServiceException {
        try {
            return ReasonCode.valueOf(mSharingInf.getReasonCode());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the direction of the sharing
     * 
     * @return Direction
     * @see Direction
     * @throws RcsServiceException
     */
    public Direction getDirection() throws RcsServiceException {
        try {
            return Direction.valueOf(mSharingInf.getDirection());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Accepts image sharing invitation
     * 
     * @throws RcsServiceException
     */
    public void acceptInvitation() throws RcsServiceException {
        try {
            mSharingInf.acceptInvitation();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Rejects image sharing invitation
     * 
     * @throws RcsServiceException
     */
    public void rejectInvitation() throws RcsServiceException {
        try {
            mSharingInf.rejectInvitation();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Aborts the sharing
     * 
     * @throws RcsServiceException
     */
    public void abortSharing() throws RcsServiceException {
        try {
            mSharingInf.abortSharing();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }
}
