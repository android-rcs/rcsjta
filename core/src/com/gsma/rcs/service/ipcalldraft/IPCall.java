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

package com.gsma.rcs.service.ipcalldraft;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;

import android.util.SparseArray;

/**
 * IP call
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCall {

    /**
     * IP call state
     */
    public enum State {

        /**
         * Call invitation received
         */
        INVITED(0),

        /**
         * Call invitation sent
         */
        INITIATED(1),

        /**
         * Call is started
         */
        STARTED(2),

        /**
         * call has been aborted
         */
        ABORTED(3),

        /**
         * Call has failed
         */
        FAILED(4),

        /**
         * Call rejected
         */
        REJECTED(5),

        /**
         * Call on hold
         */
        HOLD(6),

        /**
         * Call has been accepted and is in the process of becoming started
         */
        ACCEPTING(7),

        /**
         * Call ringing
         */
        RINGING(8);

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
     * Reason code associated with the ip call state.
     */
    public enum ReasonCode {

        /**
         * No specific reason code specified.
         */
        UNSPECIFIED(0),

        /**
         * IP call share is aborted by local user.
         */
        ABORTED_BY_USER(1),

        /**
         * IP call share is aborted by remote user.
         */
        ABORTED_BY_REMOTE(2),

        /**
         * IP call is aborted by system.
         */
        ABORTED_BY_SYSTEM(3),

        /**
         * IP call is rejected because already taken by the secondary device.
         */
        REJECTED_BY_SECONDARY_DEVICE(4),

        /**
         * IP call invitation was rejected because it is spam.
         */
        REJECTED_SPAM(5),

        /**
         * IP call invitation was rejected due to max number of sessions reached.
         */
        REJECTED_MAX_SESSIONS(6),

        /**
         * IP call invitation was rejected by local user.
         */
        REJECTED_BY_USER(7),

        /**
         * IP call invitation was rejected by remote.
         */
        REJECTED_BY_REMOTE(8),

        /**
         * IP call has been rejected due to time out.
         */
        REJECTED_TIME_OUT(9),

        /**
         * IP call initiation failed.
         */
        FAILED_INITIATION(10),

        /**
         * IP call failed.
         */
        FAILED_IPCALL(10);

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
     * IP call interface
     */
    private IIPCall callInf;

    /**
     * Constructor
     * 
     * @param callInf IP call interface
     */
    /* package private */IPCall(IIPCall callInf) {
        this.callInf = callInf;
    }

    /**
     * Returns the call ID of call
     * 
     * @return Call ID
     * @throws RcsServiceException
     */
    public String getCallId() throws RcsServiceException {
        try {
            return callInf.getCallId();
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
            return callInf.getRemoteContact();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the state of the call
     * 
     * @return State
     * @see IPCall.State
     * @throws RcsServiceException
     */
    public State getState() throws RcsServiceException {
        try {
            return State.valueOf(callInf.getState());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the reason code of the state of the call
     * 
     * @return ReasonCode
     * @see IPCall.ReasonCode
     * @throws RcsServiceException
     */
    public ReasonCode getReasonCode() throws RcsServiceException {
        try {
            return ReasonCode.valueOf(callInf.getReasonCode());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the direction of the call
     * 
     * @return Direction
     * @see Direction
     * @throws RcsServiceException
     */
    public Direction getDirection() throws RcsServiceException {
        try {
            return Direction.valueOf(callInf.getDirection());
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the timestamp of the call
     *
     * @return timestamp
     */
    public long getTimestamp() throws RcsServiceException {
        try {
            return callInf.getTimestamp();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Accepts call invitation
     * 
     * @param player IP call player
     * @param renderer IP call renderer
     * @throws RcsServiceException
     */
    public void acceptInvitation(IPCallPlayer player, IPCallRenderer renderer)
            throws RcsServiceException {
        try {
            callInf.acceptInvitation(player, renderer);
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Rejects call invitation
     * 
     * @throws RcsServiceException
     */
    public void rejectInvitation() throws RcsServiceException {
        try {
            callInf.rejectInvitation();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Aborts the call
     * 
     * @throws RcsServiceException
     */
    public void abortCall() throws RcsServiceException {
        try {
            callInf.abortCall();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Is video activated
     * 
     * @return Boolean
     * @throws RcsServiceException
     */
    public boolean isVideo() throws RcsServiceException {
        try {
            return callInf.isVideo();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Add video stream
     * 
     * @throws RcsServiceException
     */
    public void addVideo() throws RcsServiceException {
        try {
            callInf.addVideo();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Remove video stream
     * 
     * @throws RcsServiceException
     */
    public void removeVideo() throws RcsServiceException {
        try {
            callInf.removeVideo();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Is call on hold
     * 
     * @return Boolean
     * @throws RcsServiceException
     */
    public boolean isOnHold() throws RcsServiceException {
        try {
            return callInf.isOnHold();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Puts the call on hold
     * 
     * @throws RcsServiceException
     */
    public void holdCall() throws RcsServiceException {
        try {
            callInf.holdCall();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Continues the call that hold's on
     * 
     * @throws RcsServiceException
     */
    public void continueCall() throws RcsServiceException {
        try {
            callInf.continueCall();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the video codec used during sharing
     * 
     * @return VideoCodec
     * @throws RcsServiceException
     */
    public VideoCodec getVideoCodec() throws RcsServiceException {
        try {
            return callInf.getVideoCodec();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns the audio codec used during sharing
     * 
     * @return AudioCodec
     * @throws RcsServiceException
     */
    public AudioCodec getAudioCodec() throws RcsServiceException {
        try {
            return callInf.getAudioCodec();
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }
}
