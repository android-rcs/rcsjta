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

package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.RcsGenericException;

/**
 * Chat service configuration
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class ChatServiceConfiguration {

    private final IChatServiceConfiguration mIConfig;

    /**
     * Constructor
     * 
     * @param iConfig IChatServiceConfiguration instance
     * @hide
     */
    /* package private */ChatServiceConfiguration(IChatServiceConfiguration iConfig) {
        mIConfig = iConfig;
    }

    /**
     * Does the UX should alert the user that messages are handled differently when the Store and
     * Forward functionality is involved. It returns True if user should be informed when sending
     * message to offline user.
     * 
     * @return boolean
     * @throws RcsGenericException
     */
    public boolean isChatWarnSF() throws RcsGenericException {
        try {
            return mIConfig.isChatWarnSF();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the time after inactive chat could be closed
     * 
     * @return long Timeout in milliseconds
     * @throws RcsGenericException
     */
    public long getIsComposingTimeout() throws RcsGenericException {
        try {
            return mIConfig.getIsComposingTimeout();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the maximum number of participants in a group chat
     * 
     * @return int Number
     * @throws RcsGenericException
     */
    public int getGroupChatMaxParticipants() throws RcsGenericException {
        try {
            return mIConfig.getGroupChatMaxParticipants();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the minimum number of participants in a group chat
     * 
     * @return int number
     * @throws RcsGenericException
     */
    public int getGroupChatMinParticipants() throws RcsGenericException {
        try {
            return mIConfig.getGroupChatMinParticipants();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the maximum one-to-one chat message s length can have.
     * <p>
     * The length is the number of bytes of the message encoded in UTF-8.
     * 
     * @return int Number of bytes
     * @throws RcsGenericException
     */
    public int getOneToOneChatMessageMaxLength() throws RcsGenericException {
        try {
            return mIConfig.getOneToOneChatMessageMaxLength();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Return maximum length of a group chat message.
     * <p>
     * The length is the number of bytes of the message encoded in UTF-8.
     * 
     * @return int Number of bytes
     * @throws RcsGenericException
     */
    public int getGroupChatMessageMaxLength() throws RcsGenericException {
        try {
            return mIConfig.getGroupChatMessageMaxLength();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * The maximum group chat subject's length can have.
     * <p>
     * The length is the number of bytes of the message encoded in UTF-8.
     * 
     * @return int The maximum group chat subject's length can have.
     * @throws RcsGenericException
     */
    public int getGroupChatSubjectMaxLength() throws RcsGenericException {
        try {
            return mIConfig.getGroupChatSubjectMaxLength();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns True if group chat is supported, else returns False.
     * 
     * @return boolean True if group chat is supported, else returns False.
     * @throws RcsGenericException
     */
    public boolean isGroupChatSupported() throws RcsGenericException {
        try {
            return mIConfig.isGroupChatSupported();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Does the UX proposes automatically a SMS fallback in case of chat failure. It returns True if
     * SMS fallback procedure is activated, else returns False.
     * 
     * @return boolean
     * @throws RcsGenericException
     */
    public boolean isSmsFallback() throws RcsGenericException {
        try {
            return mIConfig.isSmsFallback();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Does displayed delivery report activated on received chat messages.
     * <p>
     * Only applicable to one to one chat message.
     * 
     * @return boolean
     * @throws RcsGenericException
     */
    public boolean isRespondToDisplayReportsEnabled() throws RcsGenericException {
        try {
            return mIConfig.isRespondToDisplayReportsEnabled();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Return maximum length of a geoloc label
     * 
     * @return int Number of bytes
     * @throws RcsGenericException
     */
    public int getGeolocLabelMaxLength() throws RcsGenericException {
        try {
            return mIConfig.getGeolocLabelMaxLength();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Get geoloc expiration time
     * 
     * @return int Time in milliseconds
     * @throws RcsGenericException
     */
    public long getGeolocExpirationTime() throws RcsGenericException {
        try {
            return mIConfig.getGeolocExpirationTime();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sets the parameter that controls whether to respond or not to display reports when requested
     * by the remote.<br>
     * Applicable to one to one chat messages.
     * 
     * @param enable true to set respond to display reports
     * @throws RcsGenericException
     */
    public void setRespondToDisplayReports(boolean enable) throws RcsGenericException {
        try {
            mIConfig.setRespondToDisplayReports(enable);
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }
}
