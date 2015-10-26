/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
    /**
     * Timer for expiration timeout
     */
    private Timer timer = new Timer();

    /**
     * Expiration timer task
     */
    private ExpirationTimer timerTask = null;

    /**
     * Is-composing timeout (in milliseconds)
     */
    private long mTimeout = 120000;

    /**
     * IM session
     */
    private ChatSession mSession;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(IsComposingManager.class.getSimpleName());

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
            if ((isComposingInfo != null) && isComposingInfo.isStateActive()) {
                for (ImsSessionListener sessionListener : sessionListeners) {
                    ((ChatSessionListener) sessionListener).onIsComposingEventReceived(contact,
                            true);
                }

                // Start the expiration timer
                long timeout = isComposingInfo.getRefreshTime();
                if (timeout == 0) {
                    timeout = mTimeout;
                }
                startExpirationTimer(timeout, contact);
            } else {
                for (ImsSessionListener sessionListener : sessionListeners) {
                    ((ChatSessionListener) sessionListener).onIsComposingEventReceived(contact,
                            false);
                }

                // Stop the expiration timer
                stopExpirationTimer(contact);
            }
        } catch (ParserConfigurationException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't parse is-composing event for session ID : ").append(
                    mSession.getSessionID()).toString(), e);

        } catch (SAXException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't parse is-composing event for session ID : ").append(
                    mSession.getSessionID()).toString(), e);

        } catch (ParseFailureException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't parse is-composing event for session ID : ").append(
                    mSession.getSessionID()).toString(), e);
        }
    }

    /**
     * Receive is-composing event
     * 
     * @param contact Contact identifier
     * @param state State
     */
    public void receiveIsComposingEvent(ContactId contact, boolean state) {
        // We just received an instant message, so if composing info was active, it must
        // be changed to idle. If it was already idle, no need to notify listener again
        for (int j = 0; j < mSession.getListeners().size(); j++) {
            ((ChatSessionListener) mSession.getListeners().get(j)).onIsComposingEventReceived(
                    contact, state);
        }

        // Stop the expiration timer
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
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        // Start timer
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Start is-composing timer for ").append(duration)
                    .append("ms").toString());
        }
        timerTask = new ExpirationTimer(contact);
        timer = new Timer();
        timer.schedule(timerTask, duration);
    }

    /**
     * Stop the expiration timer for a given contact
     * 
     * @param contact Contact identifier
     */
    public synchronized void stopExpirationTimer(ContactId contact) {
        // Stop timer
        if (logger.isActivated()) {
            logger.debug("Stop is-composing timer");
        }

        // TODO : stop timer for a given contact
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    /**
     * Internal expiration timer
     */
    private class ExpirationTimer extends TimerTask {

        private ContactId contact;

        public ExpirationTimer(ContactId contact) {
            this.contact = contact;
        }

        public void run() {
            if (logger.isActivated()) {
                logger.debug("Is-composing timer has expired: " + contact
                        + " is now considered idle");
            }
            for (ImsSessionListener sessionListener : mSession.getListeners()) {
                ((ChatSessionListener) sessionListener).onIsComposingEventReceived(contact, false);
            }
        }
    }
}
