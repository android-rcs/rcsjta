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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Chat service configuration
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatServiceConfiguration {
	/**
	 * IM always-on thanks to the Store & Forward
	 */
	private boolean imAlwaysOn;
	
	/**
	 * Store and Forward warning
	 */
	private boolean warnSF;
	
	/**
	 * Chat timeout
	 */
	private int chatTimeout;
	
	/**
	 * Is-composing timeout
	 */
	private int isComposingTimeout;

	/**
	 * Max participants in a group chat
	 */
	private int maxGroupChatParticipants;
	
	/**
	 * Max length of a message in a one-to-one chat
	 */
	private int mMaxMsgLengthOneToOneChat;

	/**
	 * Max length of a message in a group chat
	 */
	private int maxMsgLengthGroupChat;
	
	/**
	 * SMS fallback
	 */
	private boolean smsFallback;

	/**
	 * Respond to displayed delivery report
	 */
	private boolean respondToDisplayReports;
	
	/**
	 * Max geoloc label length
	 */
	private int maxGeolocLabelLength;

	/**
	 * Geoloc expiration time
	 */
	private int geolocExpireTime;

	private int minGroupChatParticipants;

	/**
	 * The maximum length a group chat subject can have.
	 * <p>The length is the number of bytes of the message encoded in UTF-8.
	 */
	private int groupChatSubjectMaxLength;
	
	/**
	 * Constructor
	 * 
	 * @param imAlwaysOn IM always-on thanks to the Store & Forward
	 * @param warnSF Store and Forward warning
	 * @param chatTimeout Chat timeout
	 * @param isComposingTimeout Is-composing timeout
	 * @param maxGroupChatParticipants Max participants in a group chat
	 * @param minGroupChatParticipants Min participants in a group chat
	 * @param maxMsgLengthOneToOneChat Max length of a message in a one-to-one chat
	 * @param maxMsgLengthGroupChat Max length of a message in a group chat
	 * @param groupChatSubjectMaxLength Max length of subject in a group chat
	 * @param smsFallback SMS fallback
	 * @param respondToDisplayReports Respond to displayed delivery report
	 * @param maxGeolocLabelLength Max geoloc label length
	 * @param geolocExpireTime Geoloc expiration time
     * @hide
	 */
	public ChatServiceConfiguration(boolean imAlwaysOn, boolean warnSF, int chatTimeout, int isComposingTimeout,
			int maxGroupChatParticipants, int minGroupChatParticipants, int maxMsgLengthOneToOneChat, int maxMsgLengthGroupChat,
			int groupChatSubjectMaxLength, boolean smsFallback, boolean respondToDisplayReports, int maxGeolocLabelLength,
			int geolocExpireTime) {
		this.imAlwaysOn = imAlwaysOn;
		this.warnSF = warnSF;
		this.chatTimeout = chatTimeout;
		this.isComposingTimeout = isComposingTimeout;
		this.maxGroupChatParticipants = maxGroupChatParticipants;
		this.minGroupChatParticipants = minGroupChatParticipants;
		mMaxMsgLengthOneToOneChat = maxMsgLengthOneToOneChat;
		this.maxMsgLengthGroupChat = maxMsgLengthGroupChat;
		this.groupChatSubjectMaxLength = groupChatSubjectMaxLength;
		this.smsFallback = smsFallback;
		this.respondToDisplayReports = respondToDisplayReports;
		this.maxGeolocLabelLength = maxGeolocLabelLength;
		this.geolocExpireTime = geolocExpireTime;
    }	
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public ChatServiceConfiguration(Parcel source) {
		this.imAlwaysOn = source.readInt() != 0;
		this.warnSF = source.readInt() != 0;
		this.chatTimeout = source.readInt();
		this.isComposingTimeout = source.readInt();
		this.maxGroupChatParticipants = source.readInt();
		this.minGroupChatParticipants = source.readInt();
		this.maxMsgLengthGroupChat = source.readInt();
		this.groupChatSubjectMaxLength = source.readInt();
		this.smsFallback = source.readInt() != 0;
		this.respondToDisplayReports = source.readInt() != 0;
		this.maxGeolocLabelLength = source.readInt();
		this.geolocExpireTime = source.readInt();
    }

	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
	 * 
	 * @return Integer
     * @hide
	 */
	public int describeContents() {
        return 0;
    }

	/**
	 * Write parcelable object
	 * 
	 * @param dest The Parcel in which the object should be written
	 * @param flags Additional flags about how the object should be written
     * @hide
	 */
    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeInt(imAlwaysOn ? 1 : 0);
    	dest.writeInt(warnSF ? 1 : 0);
    	dest.writeInt(chatTimeout);
    	dest.writeInt(isComposingTimeout);
    	dest.writeInt(maxGroupChatParticipants);
    	dest.writeInt(minGroupChatParticipants);
    	dest.writeInt(mMaxMsgLengthOneToOneChat);
    	dest.writeInt(maxMsgLengthGroupChat);
    	dest.writeInt(groupChatSubjectMaxLength);
    	dest.writeInt(smsFallback ? 1 : 0);
    	dest.writeInt(respondToDisplayReports ? 1 : 0);
    	dest.writeInt(maxGeolocLabelLength);
    	dest.writeInt(geolocExpireTime);
    }

    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<ChatServiceConfiguration> CREATOR
            = new Parcelable.Creator<ChatServiceConfiguration>() {
        public ChatServiceConfiguration createFromParcel(Parcel source) {
            return new ChatServiceConfiguration(source);
        }

        public ChatServiceConfiguration[] newArray(int size) {
            return new ChatServiceConfiguration[size];
        }
    };	
	
	/**
	 * Is the Store and Forward capability is supported.
	 * 
	 * @return True if Store and Forward capability is supported, False if no Store & Forward capability
	 */
	public boolean isChatSf() {
		return imAlwaysOn;
	}

	/**
	 * Does the UX should alert the user that messages are handled differently when
	 * the Store and Forward functionality is involved. It returns True if user should
	 * be informed when sending message to offline user.
	 * <p>This should be used with isChatSf.
	 * 
	 * @return Boolean
	 */
	public boolean isChatWarnSF() {
		return warnSF;
	}
	
	/**
	 * Returns the time after inactive chat could be closed
	 * 
	 * @return Timeout in seconds
	 */
	public int getChatTimeout() {
		return chatTimeout;
	}
	
	/**
	 * Returns the time after an inactive chat could be closed
	 * 
	 * @return Timeout in seconds
	 */
	public int getIsComposingTimeout() {
		return isComposingTimeout;
	}	
	
	/**
	 * Returns the maximum number of participants in a group chat
	 * 
	 * @return Number
	 */
	public int getGroupChatMaxParticipants() {
		return maxGroupChatParticipants;
	}
	
	/**
	 * Returns the minimum number of participants in a group chat
	 * @return number
	 */
	public int getGroupChatMinParticipants() {
	  return minGroupChatParticipants;	
	}
	
	/**
	 * Returns the maximum one-to-one chat message’s length can have.
	 * <p>
	 * The length is the number of bytes of the message encoded in UTF-8.
	 * 
	 * @return Number of bytes
	 */
	public int getOneToOneChatMessageMaxLength() {
		return mMaxMsgLengthOneToOneChat;
	}
	
	/**
	 * Return maximum length of a group chat message.
	 * <p>
	 * The length is the number of bytes of the message encoded in UTF-8.
	 * 
	 * @return Number of bytes
	 */
	public int getGroupChatMessageMaxLength() {
		return maxMsgLengthGroupChat;
	}
	
	/**
	 * The maximum group chat subject's length can have.
	 * <p>The length is the number of bytes of the message encoded in UTF-8.
	 * @return The maximum group chat subject's length can have.
	 */
	public int getGroupChatSubjectMaxLength() {
		return groupChatSubjectMaxLength;
	}

	/**
	 * Does the UX proposes automatically a SMS fallback in case of chat failure. It
	 * returns True if SMS fallback procedure is activated, else returns False.
	 * 
	 * @return Boolean
	 */
	public boolean isSmsFallback() {
		return smsFallback;
	}
	
	/**
	 * Does displayed delivery report activated on received chat messages.
	 * <p>
	 * Only applicable to one to one chat message.
	 * 
	 * @return Boolean
	 */
	public boolean isRespondToDisplayReportsEnabled() {
		return respondToDisplayReports;
	}

	/**
	 * Return maximum length of a geoloc label
	 * 
	 * @return Number of bytes
	 */
	public int getGeolocLabelMaxLength() {
		return maxGeolocLabelLength;
	}

    /**
     * Get geoloc expiration time
     *
     * @return Time in seconds
     */
	public int getGeolocExpirationTime() {
		return geolocExpireTime;
	}
}
