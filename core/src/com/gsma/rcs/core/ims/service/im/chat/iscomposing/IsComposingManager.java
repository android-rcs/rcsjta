/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat.iscomposing;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Is Composing manager which manages "is composing" events as per RFC3994. It handles the status
 * (idle or active) of contact according to received messages and timers.
 */
public class IsComposingManager {

    private static final long DEFAULT_REFESH_TIMEOUT = 120000;

    /**
     * Expiration timer task
     */
    private ExpirationTimer mTimerTask;

    private final ChatSession mSession;

    private static final Logger sLogger = Logger.getLogger(IsComposingManager.class.getName());

    /**
     * Constructor
     *
     * @param session IM session
     */
    public IsComposingManager(ChatSession session) {
        mSession = session;
    }

    /**
     * Receive is-composing event
     *
     * @param contact Contact identifier
     * @param event Event
     * @throws PayloadException
     */
    public void receiveIsComposingEvent(ContactId contact, byte[] event) throws PayloadException {
        try {
            InputSource input = new InputSource(new ByteArrayInputStream(event));
            IsComposingParser parser = new IsComposingParser(input).parse();
            IsComposingInfo isComposingInfo = parser.getIsComposingInfo();
            List<ImsSessionListener> sessionListeners = mSession.getListeners();
            if (isComposingInfo != null && isComposingInfo.isStateActive()) {
                for (ImsSessionListener sessionListener : sessionListeners) {
                    ((ChatSessionListener) sessionListener).onIsComposingEventReceived(contact,
                            true);
                }
                long timeout = isComposingInfo.getRefreshTime();
                if (timeout == 0) {
                    timeout = DEFAULT_REFESH_TIMEOUT;
                }
                startExpirationTimer(timeout, contact);
            } else {
                for (ImsSessionListener sessionListener : sessionListeners) {
                    ((ChatSessionListener) sessionListener).onIsComposingEventReceived(contact,
                            false);
                }
                stopExpirationTimer(contact);
            }
        } catch (ParserConfigurationException | SAXException | ParseFailureException e) {
            throw new PayloadException("Can't parse is-composing event for session ID : "
                    + mSession.getSessionID(), e);
        }
    }

    /**
     * Receive is-composing event
     *
     * @param contact Contact identifier
     * @param state State
     */
    public void receiveIsComposingEvent(ContactId contact, boolean state) {
        /*
         * We just received an instant message, so if composing info was active, it must be changed
         * to idle. If it was already idle, no need to notify listener again.
         */
        for (ImsSessionListener listener : mSession.getListeners()) {
            ((ChatSessionListener) listener).onIsComposingEventReceived(contact, state);
        }
        stopExpirationTimer(contact);
    }

    /**
     * Start the expiration timer for a given contact
     *
     * @param duration Timer period in milliseconds
     * @param contact Contact identifier
     */
    public synchronized void startExpirationTimer(long duration, ContactId contact) {
        // Remove old timer
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        // Start timer
        if (sLogger.isActivated()) {
            sLogger.debug("Start is-composing timer for " + duration + "ms");
        }
        mTimerTask = new ExpirationTimer(contact);
        new Timer().schedule(mTimerTask, duration);
    }

    /**
     * Stop the expiration timer for a given contact
     *
     * @param contact Contact identifier
     */
    public synchronized void stopExpirationTimer(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Stop is-composing timer");
        }
        // TODO : stop timer for a given contact
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    /**
     * Internal expiration timer
     */
    private class ExpirationTimer extends TimerTask {

        private ContactId mContact;

        public ExpirationTimer(ContactId contact) {
            mContact = contact;
        }

        public void run() {
            if (sLogger.isActivated()) {
                sLogger.debug("Is-composing timer has expired: " + mContact
                        + " is now considered idle");
            }
            for (ImsSessionListener sessionListener : mSession.getListeners()) {
                ((ChatSessionListener) sessionListener).onIsComposingEventReceived(mContact, false);
            }
        }
    }
}
