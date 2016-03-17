/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService.Direction;
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

        private static SparseArray<State> mValueToEnum = new SparseArray<>();
        static {
            for (State entry : State.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        State(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public static State valueOf(int value) {
            State entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + State.class.getName() + ""
                    + value + "!");
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
         * Incoming image was rejected by system.
         */
        REJECTED_BY_SYSTEM(12),

        /**
         * Image share initiation failed;
         */
        FAILED_INITIATION(13),

        /**
         * Sharing of the image share has failed.
         */
        FAILED_SHARING(14),

        /**
         * Saving of the image share has failed.
         */
        FAILED_SAVING(15);

        private final int mValue;

        private static SparseArray<ReasonCode> mValueToEnum = new SparseArray<>();
        static {
            for (ReasonCode entry : ReasonCode.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        ReasonCode(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public static ReasonCode valueOf(int value) {
            ReasonCode entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + ReasonCode.class.getName()
                    + "" + value + "!");
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
     * @return String Sharing ID
     * @throws RcsGenericException
     */
    public String getSharingId() throws RcsGenericException {
        try {
            return mSharingInf.getSharingId();

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
            return mSharingInf.getRemoteContact();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the URI of the file to be shared
     * 
     * @return Uri
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public Uri getFile() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mSharingInf.getFile();

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
            return mSharingInf.getFileName();

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
            return mSharingInf.getFileSize();

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
            return mSharingInf.getMimeType();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the local timestamp of when the image sharing was initiated for outgoing image
     * sharing or the local timestamp of when the image sharing invitation was received for incoming
     * image sharings.
     * 
     * @return long
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public long getTimestamp() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return mSharingInf.getTimestamp();

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the state of the sharing
     * 
     * @return State
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     * @see ImageSharing.State
     */
    public State getState() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return State.valueOf(mSharingInf.getState());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the reason code of the state of the sharing
     * 
     * @return ReasonCode
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     * @see ImageSharing.ReasonCode
     */
    public ReasonCode getReasonCode() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return ReasonCode.valueOf(mSharingInf.getReasonCode());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the direction of the sharing
     * 
     * @return Direction
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     * @see Direction
     */
    public Direction getDirection() throws RcsPersistentStorageException, RcsGenericException {
        try {
            return Direction.valueOf(mSharingInf.getDirection());

        } catch (Exception e) {
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Accepts image sharing invitation
     * 
     * @throws RcsGenericException
     */
    public void acceptInvitation() throws RcsGenericException {
        try {
            mSharingInf.acceptInvitation();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Rejects image sharing invitation
     * 
     * @throws RcsGenericException
     */
    public void rejectInvitation() throws RcsGenericException {
        try {
            mSharingInf.rejectInvitation();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Aborts the sharing
     * 
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void abortSharing() throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mSharingInf.abortSharing();
        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
