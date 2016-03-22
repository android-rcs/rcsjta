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
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService.Direction;
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
    private IMultimediaMessagingSession mSessionIntf;

    /**
     * Constructor
     * 
     * @param sessionIntf Multimedia session interface
     */
    /* package private */MultimediaMessagingSession(IMultimediaMessagingSession sessionIntf) {
        super();
        mSessionIntf = sessionIntf;
    }

    /**
     * Returns the session ID of the multimedia session
     * 
     * @return String Session ID
     * @throws RcsGenericException
     */
    public String getSessionId() throws RcsGenericException {
        try {
            return mSessionIntf.getSessionId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the remote contact identifier
     * 
     * @return ContactId
     * @throws RcsGenericException
     */
    public ContactId getRemoteContact() throws RcsGenericException {
        try {
            return mSessionIntf.getRemoteContact();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the service ID
     * 
     * @return String Service ID
     * @throws RcsGenericException
     */
    public String getServiceId() throws RcsGenericException {
        try {
            return mSessionIntf.getServiceId();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the state of the session
     * 
     * @return State
     * @see MultimediaSession.State
     * @throws RcsGenericException
     */
    public State getState() throws RcsGenericException {
        try {
            return State.valueOf(mSessionIntf.getState());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the reason code state of the session
     * 
     * @return ReasonCode
     * @see MultimediaSession.ReasonCode
     * @throws RcsGenericException
     */
    public ReasonCode getReasonCode() throws RcsGenericException {
        try {
            return ReasonCode.valueOf(mSessionIntf.getReasonCode());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the direction of the session
     * 
     * @return Direction
     * @see Direction
     * @throws RcsGenericException
     */
    public Direction getDirection() throws RcsGenericException {
        try {
            return Direction.valueOf(mSessionIntf.getDirection());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Accepts session invitation.
     * 
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void acceptInvitation() throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mSessionIntf.acceptInvitation();

        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Rejects session invitation
     * 
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void rejectInvitation() throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mSessionIntf.rejectInvitation();

        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Aborts the session
     * 
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void abortSession() throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mSessionIntf.abortSession();

        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a message in real time
     * 
     * @deprecated Use {@link #sendMessage(byte[] content, String contentType)} instead.
     * @param content Message content
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    @Deprecated
    public void sendMessage(byte[] content) throws RcsPermissionDeniedException,
            RcsGenericException {
        try {
            mSessionIntf.sendMessage(content);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sends a message in real time
     *
     * @param content Message content
     * @param contentType Message content type
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void sendMessage(byte[] content, String contentType)
            throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mSessionIntf.sendMessage2(content, contentType);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Flush all messages of the session
     *
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void flushMessages() throws RcsPermissionDeniedException, RcsGenericException {
        try {
            mSessionIntf.flushMessages();

        } catch (Exception e) {
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
