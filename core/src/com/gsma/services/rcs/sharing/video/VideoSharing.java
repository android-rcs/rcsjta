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

package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;

import android.util.SparseArray;

/**
 * Video sharing
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharing {

    /**
     * Video sharing state
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
         * Sharing has been rejected
         */
        REJECTED(5),

        /**
         * Ringing
         */
        RINGING(6),

        /**
         * Sharing has been accepted and is in the process of becoming started
         */
        ACCEPTING(7);

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
     * Reason code associated with the video share state.
     */
    public enum ReasonCode {

        /**
         * No specific reason code specified.
         */
        UNSPECIFIED(0),

        /**
         * Video share is aborted by local user.
         */
        ABORTED_BY_USER(1),

        /**
         * Video share is aborted by remote user.
         */
        ABORTED_BY_REMOTE(2),

        /**
         * Video share is aborted by system.
         */
        ABORTED_BY_SYSTEM(3),

        /**
         * Video share is rejected because already taken by the secondary device.
         */
        REJECTED_BY_SECONDARY_DEVICE(4),

        /**
         * Video share invitation was rejected due to max number of sharing sessions already are
         * open.
         */
        REJECTED_MAX_SHARING_SESSIONS(5),

        /**
         * Video share invitation was rejected by local user.
         */
        REJECTED_BY_USER(6),

        /**
         * Video share invitation was rejected by remote.
         */
        REJECTED_BY_REMOTE(7),

        /**
         * Video share been rejected due to inactivity.
         */
        REJECTED_BY_INACTIVITY(8),

        /**
         * Video share initiation failed.
         */
        FAILED_INITIATION(9),

        /**
         * Sharing of the video share has failed.
         */
        FAILED_SHARING(10);

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
     * Video encoding
     */
    public static class Encoding {
        /**
         * H264
         */
        public static final String H264 = "H264";
    }

    /**
     * Video sharing interface
     */
    private final IVideoSharing mSharingInf;

    /**
     * Constructor
     * 
     * @param sharingInf Video sharing interface
     */
    /* package private */VideoSharing(IVideoSharing sharingInf) {
        mSharingInf = sharingInf;
    }

    /**
     * Returns the sharing ID of the video sharing
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
     * Returns the state of the sharing
     * 
     * @return State
     * @see VideoSharing.State
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
     * Returns the reason code of the sharing
     * 
     * @return ReasonCode
     * @see VideoSharing.ReasonCode
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
     * Accepts video sharing invitation
     * 
     * @param player Video player
     * @throws RcsServiceException
     */
    public void acceptInvitation(VideoPlayer player) throws RcsServiceException {
        try {
            mSharingInf.acceptInvitation(player);
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Rejects video sharing invitation
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

    /**
     * Return the video encoding (eg. H.264)
     * 
     * @return Encoding
     * @throws RcsServiceException
     */
    public String getVideoEncoding() throws RcsServiceException {
        try {
            return mSharingInf.getVideoEncoding();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the local timestamp of when the video sharing was initiated for outgoing video
     * sharing or the local timestamp of when the video sharing invitation was received for incoming
     * video sharings.
     * 
     * @return Timestamp in milliseconds
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
     * Returns the duration of the video sharing
     * 
     * @return Duration in milliseconds
     * @throws RcsServiceException
     */
    public long getDuration() throws RcsServiceException {
        try {
            return mSharingInf.getDuration();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the video descriptor
     * 
     * @return Video descriptor
     * @see VideoDescriptor
     * @throws RcsServiceException
     */
    public VideoDescriptor getVideoDescriptor() throws RcsServiceException {
        try {
            return mSharingInf.getVideoDescriptor();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }
}
