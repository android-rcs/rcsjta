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
package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.RcsServiceException;

/**
 * Chat service configuration
 * 
 * @author Jean-Marc AUFFRET
 * @author yplo6403
 *
 */
public class ChatServiceConfiguration {

	private final IChatServiceConfiguration mIConfig;

	/**
	 * Constructor
	 * 
	 * @param iConfig
	 *            IChatServiceConfiguration instance
	 * @hide
	 */
	/* package private */ChatServiceConfiguration(IChatServiceConfiguration iConfig) {
		mIConfig = iConfig;
	}

	/**
	 * Is the Store and Forward capability is supported.
	 * 
	 * @return True if Store and Forward capability is supported, False if no Store & Forward
	 *         capability
	 * @throws RcsServiceException
	 */
	public boolean isChatSf() throws RcsServiceException {
		try {
			return mIConfig.isChatSf();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Does the UX should alert the user that messages are handled differently when the Store and
	 * Forward functionality is involved. It returns True if user should be informed when sending
	 * message to offline user.
	 * <p>
	 * This should be used with isChatSf.
	 * 
	 * @return Boolean
	 * @throws RcsServiceException
	 */
	public boolean isChatWarnSF() throws RcsServiceException {
		try {
			return mIConfig.isChatWarnSF();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the time after inactive chat could be closed
	 * 
	 * @return Timeout in seconds
	 * @throws RcsServiceException
	 */
	public int getChatTimeout() throws RcsServiceException {
		try {
			return mIConfig.getChatTimeout();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the time after an inactive chat could be closed
	 * 
	 * @return Timeout in seconds
	 * @throws RcsServiceException
	 */
	public int getIsComposingTimeout() throws RcsServiceException {
		try {
			return mIConfig.getIsComposingTimeout();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the maximum number of participants in a group chat
	 * 
	 * @return Number
	 * @throws RcsServiceException
	 */
	public int getGroupChatMaxParticipants() throws RcsServiceException {
		try {
			return mIConfig.getGroupChatMaxParticipants();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the minimum number of participants in a group chat
	 * 
	 * @return number
	 * @throws RcsServiceException
	 */
	public int getGroupChatMinParticipants() throws RcsServiceException {
		try {
			return mIConfig.getGroupChatMinParticipants();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the maximum one-to-one chat message s length can have.
	 * <p>
	 * The length is the number of bytes of the message encoded in UTF-8.
	 * 
	 * @return Number of bytes
	 * @throws RcsServiceException
	 */
	public int getOneToOneChatMessageMaxLength() throws RcsServiceException {
		try {
			return mIConfig.getOneToOneChatMessageMaxLength();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Return maximum length of a group chat message.
	 * <p>
	 * The length is the number of bytes of the message encoded in UTF-8.
	 * 
	 * @return Number of bytes
	 * @throws RcsServiceException
	 */
	public int getGroupChatMessageMaxLength() throws RcsServiceException {
		try {
			return mIConfig.getGroupChatMessageMaxLength();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * The maximum group chat subject's length can have.
	 * <p>
	 * The length is the number of bytes of the message encoded in UTF-8.
	 * 
	 * @return The maximum group chat subject's length can have.
	 * @throws RcsServiceException
	 */
	public int getGroupChatSubjectMaxLength() throws RcsServiceException {
		try {
			return mIConfig.getGroupChatSubjectMaxLength();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns True if group chat is supported, else returns False.
	 * 
	 * @return True if group chat is supported, else returns False.
	 * @throws RcsServiceException
	 */
	public boolean isGroupChatSupported() throws RcsServiceException {
		try {
			return mIConfig.isGroupChatSupported();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Does the UX proposes automatically a SMS fallback in case of chat failure. It returns True if
	 * SMS fallback procedure is activated, else returns False.
	 * 
	 * @return Boolean
	 * @throws RcsServiceException
	 */
	public boolean isSmsFallback() throws RcsServiceException {
		try {
			return mIConfig.isSmsFallback();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Does displayed delivery report activated on received chat messages.
	 * <p>
	 * Only applicable to one to one chat message.
	 * 
	 * @return Boolean
	 * @throws RcsServiceException
	 */
	public boolean isRespondToDisplayReportsEnabled() throws RcsServiceException {
		try {
			return mIConfig.isRespondToDisplayReportsEnabled();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Return maximum length of a geoloc label
	 * 
	 * @return Number of bytes
	 * @throws RcsServiceException
	 */
	public int getGeolocLabelMaxLength() throws RcsServiceException {
		try {
			return mIConfig.getGeolocLabelMaxLength();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Get geoloc expiration time
	 *
	 * @return Time in seconds
	 * @throws RcsServiceException
	 */
	public int getGeolocExpirationTime() throws RcsServiceException {
		try {
			return mIConfig.getGeolocExpirationTime();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Sets the parameter that controls whether to respond or not to display reports when requested
	 * by the remote.<br>
	 * Applicable to one to one chat messages.
	 * 
	 * @param enable
	 * @throws RcsServiceException
	 */
	public void setRespondToDisplayReports(boolean enable) throws RcsServiceException {
		try {
			mIConfig.setRespondToDisplayReports(enable);
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}
}
