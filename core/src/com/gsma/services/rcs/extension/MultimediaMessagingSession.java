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

package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;

/**
 * This class maintains the information related to a multimedia session for a real time messaging
 * service.
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaMessagingSession extends MultimediaSession {
    /**
     * Messaging session interface
     */
    private IMultimediaMessagingSession sessionIntf;

    /**
     * Constructor
     * 
     * @param sessionInf Multimedia session interface
     */
    MultimediaMessagingSession(IMultimediaMessagingSession sessionIntf) {
        super();

        this.sessionIntf = sessionIntf;
    }

    /**
     * Returns the session ID of the multimedia session
     * 
     * @return Session ID
     * @throws RcsServiceException
     */
    public String getSessionId() throws RcsServiceException {
        try {
            return sessionIntf.getSessionId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
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
            return sessionIntf.getRemoteContact();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the service ID
     * 
     * @return Service ID
     * @throws RcsServiceException
     */
    public String getServiceId() throws RcsServiceException {
        try {
            return sessionIntf.getServiceId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the state of the session
     * 
     * @return State
     * @see MultimediaSession.State
     * @throws RcsServiceException
     */
    public State getState() throws RcsServiceException {
        try {
            return State.valueOf(sessionIntf.getState());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the reason code state of the session
     * 
     * @return ReasonCode
     * @see MultimediaSession.ReasonCode
     * @throws RcsServiceException
     */
    public ReasonCode getReasonCode() throws RcsServiceException {
        try {
            return ReasonCode.valueOf(sessionIntf.getReasonCode());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the direction of the session
     * 
     * @return Direction
     * @see Direction
     * @throws RcsServiceException
     */
    public Direction getDirection() throws RcsServiceException {
        try {
            return Direction.valueOf(sessionIntf.getDirection());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Accepts session invitation.
     * 
     * @throws RcsServiceException
     */
    public void acceptInvitation() throws RcsServiceException {
        try {
            sessionIntf.acceptInvitation();
        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Rejects session invitation
     * 
     * @throws RcsServiceException
     */
    public void rejectInvitation() throws RcsServiceException {
        try {
            sessionIntf.rejectInvitation();
        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Aborts the session
     * 
     * @throws RcsServiceException
     */
    public void abortSession() throws RcsServiceException {
        try {
            sessionIntf.abortSession();
        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a message in real time
     * 
     * @param content Message content
     * @throws RcsServiceException
     */
    public void sendMessage(byte[] content) throws RcsServiceException {
        try {
            sessionIntf.sendMessage(content);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
