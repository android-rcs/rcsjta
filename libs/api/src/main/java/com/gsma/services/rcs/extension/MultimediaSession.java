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

package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;

import android.util.SparseArray;

/**
 * This class maintains the information related to a multimedia session and offers methods to manage
 * it. This is an abstract class between a messaging session and a streaming session.
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class MultimediaSession {
    /**
     * Multimedia session state
     */
    public enum State {

        /**
         * Session invitation received
         */
        INVITED(0),

        /**
         * Session invitation sent
         */
        INITIATING(1),

        /**
         * Session is started
         */
        STARTED(2),

        /**
         * Session has been aborted
         */
        ABORTED(3),

        /**
         * Session has failed
         */
        FAILED(4),

        /**
         * Session has been rejected.
         */
        REJECTED(5),

        /**
         * Call ringing
         */
        RINGING(6),

        /**
         * Session has been accepted and is in the process of becoming started
         */
        ACCEPTING(7);

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
                    + value);
        }
    }

    /**
     * Multimedia session reason code
     */
    public enum ReasonCode {

        /**
         * No specific reason code specified.
         */
        UNSPECIFIED(0),

        /**
         * Session is aborted by local user.
         */
        ABORTED_BY_USER(1),

        /**
         * Session is aborted by remote user.
         */
        ABORTED_BY_REMOTE(2),

        /**
         * Session is aborted by system.
         */
        ABORTED_BY_SYSTEM(3),

        /**
         * Session is aborted by inactivity.
         */
        ABORTED_BY_INACTIVITY(4),

        /**
         * Session invitation was rejected by local user.
         */
        REJECTED_BY_USER(5),

        /**
         * Session invitation was rejected by remote.
         */
        REJECTED_BY_REMOTE(6),

        /**
         * Session invitation was rejected by timeout.
         */
        REJECTED_BY_TIMEOUT(7),

        /**
         * Session invitation was rejected by system.
         */
        REJECTED_BY_SYSTEM(8),

        /**
         * Initiation failed.
         */
        FAILED_INITIATION(9),

        /**
         * Session failed.
         */
        FAILED_SESSION(10),

        /**
         * Media failed.
         */
        FAILED_MEDIA(11);

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
                    + "" + value);
        }
    }

    /**
     * Constructor
     */
    MultimediaSession() {
    }

    /**
     * Returns the session ID of the multimedia session
     * 
     * @return String Session ID
     * @throws RcsGenericException
     */
    public abstract String getSessionId() throws RcsGenericException;

    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     * @throws RcsGenericException
     */
    public abstract ContactId getRemoteContact() throws RcsGenericException;

    /**
     * Returns the service ID
     * 
     * @return String Service ID
     * @throws RcsGenericException
     */
    public abstract String getServiceId() throws RcsGenericException;

    /**
     * Returns the state of the session
     * 
     * @return State
     * @see MultimediaSession.State
     * @throws RcsGenericException
     */
    public abstract State getState() throws RcsGenericException;

    /**
     * Returns the direction of the session
     * 
     * @return Direction
     * @see Direction
     * @throws RcsGenericException
     */
    public abstract Direction getDirection() throws RcsGenericException;

    /**
     * Returns the reason code of the session.
     * 
     * @return ReasonCode
     * @see MultimediaSession.ReasonCode
     * @throws RcsGenericException
     */
    public abstract ReasonCode getReasonCode() throws RcsGenericException;

    /**
     * Accepts session invitation.
     * 
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public abstract void acceptInvitation() throws RcsPermissionDeniedException,
            RcsGenericException;

    /**
     * Rejects session invitation
     * 
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public abstract void rejectInvitation() throws RcsPermissionDeniedException,
            RcsGenericException;

    /**
     * Aborts the session
     * 
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public abstract void abortSession() throws RcsPermissionDeniedException, RcsGenericException;
}
