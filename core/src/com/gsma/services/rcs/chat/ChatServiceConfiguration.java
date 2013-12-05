/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
	 * Max length of a message in a single chat
	 */
	private int maxMsgLengthSingleChat;

	/**
	 * Max length of a message in a group chat
	 */
	private int maxMsgLengthGroupChat;

	/**
	 * Max group chat sessions
	 */
	private int maxGroupChat; 
	
	/**
	 * SMS fallback
	 */
	private boolean smsFallback;

	/**
	 * Auto accept mode for single chat
	 */
	private boolean autoAcceptSingleChat;

	/**
	 * Auto accept mode for group chat
	 */
	private boolean autoAcceptGroupChat;
	
	/**
	 * Displayed delivery report
	 */
	private boolean displayedDeliveryReport;
	
	/**
	 * Max geoloc label length
	 */
	private int maxGeolocLabelLength;

	/**
	 * Geoloc expiraion time
	 */
	private int geolocExpireTime;
	
	/**
	 * Constructor
	 * 
	 * @param chatSF Chat S&F
	 * @param groupChatSF Group chat S&F
	 * @param warnSF Store and Forward warning
	 * @param chatTimeout Chat timeout
	 * @param isComposingTimeout Is-composing timeout
	 * @param maxGroupChatParticipants Max participants in a group chat
	 * @param maxMsgLengthSingleChat Max length of a message in a single chat
	 * @param maxMsgLengthGroupChat Max length of a message in a group chat
	 * @param maxGroupChat Max group chats
	 * @param smsFallback SMS fallback
	 * @param autoAcceptSingleChat Auto accept mode for single chat
	 * @param autoAcceptGroupChat Auto accept mode for group chat
	 * @param displayedDeliveryReport Displayed delivery report
	 * @param maxGeolocLabelLength Max geoloc label length
	 * @param geolocExpireTime Geoloc expiration time
     * @hide
	 */
	public ChatServiceConfiguration(boolean warnSF, int chatTimeout, int isComposingTimeout,
			int maxGroupChatParticipants, int maxMsgLengthSingleChat, int maxMsgLengthGroupChat,
			int maxGroupChat, boolean smsFallback, boolean autoAcceptSingleChat, boolean autoAcceptGroupChat,
			boolean displayedDeliveryReport, int maxGeolocLabelLength, int geolocExpireTime) {
		this.warnSF = warnSF;
		this.chatTimeout = chatTimeout;
		this.isComposingTimeout = isComposingTimeout;
		this.maxGroupChatParticipants = maxGroupChatParticipants;
		this.maxMsgLengthSingleChat = maxMsgLengthSingleChat;
		this.maxMsgLengthGroupChat = maxMsgLengthGroupChat;
		this.maxGroupChat = maxGroupChat;
		this.smsFallback = smsFallback;
		this.autoAcceptSingleChat = autoAcceptSingleChat;
		this.autoAcceptGroupChat = autoAcceptGroupChat;
		this.displayedDeliveryReport = displayedDeliveryReport;
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
		this.chatTimeout = source.readInt();
		this.isComposingTimeout = source.readInt();
		this.maxGroupChatParticipants = source.readInt();
		this.maxMsgLengthSingleChat = source.readInt();
		this.maxMsgLengthGroupChat = source.readInt();
		this.smsFallback = source.readInt() != 0;
		this.autoAcceptSingleChat = source.readInt() != 0;
		this.autoAcceptGroupChat = source.readInt() != 0;
		this.displayedDeliveryReport = source.readInt() != 0;
		this.warnSF = source.readInt() != 0;
		this.maxGroupChat = source.readInt(); 
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
    	dest.writeInt(chatTimeout);
    	dest.writeInt(isComposingTimeout);
    	dest.writeInt(maxGroupChatParticipants);
    	dest.writeInt(maxMsgLengthSingleChat);
    	dest.writeInt(maxMsgLengthGroupChat);
    	dest.writeInt(smsFallback ? 1 : 0);
    	dest.writeInt(autoAcceptSingleChat ? 1 : 0);
    	dest.writeInt(autoAcceptGroupChat ? 1 : 0);
    	dest.writeInt(displayedDeliveryReport ? 1 : 0);
    	dest.writeInt(warnSF ? 1 : 0);
    	dest.writeInt(maxGroupChat);
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
	 * Does the UX should alert the user that messages are handled differently when
	 * the Store and Forward functionality is involved. It returns True if user should
	 * be informed when sending message to offline user.
	 * 
	 * @return Boolean
	 */
	public boolean isChatWarnSF() {
		return warnSF;
	}
	
	/**
	 * Returns the time after inactive chat session could be closed
	 * 
	 * @return Timeout in seconds
	 */
	public int getChatTimeout() {
		return chatTimeout;
	}
	
	/**
	 * Returns the Is-composing timeout value
	 * 
	 * @return Timeout in seconds
	 */
	public int getIsComposingTimeout() {
		return isComposingTimeout;
	}	
	
	/**
	 * Returns maximum number of participants in a group chat
	 * 
	 * @return Number
	 */
	public int getGroupChatMaxParticipantsNumber() {
		return maxGroupChatParticipants;
	}
	
	/**
	 * Return maximum length of a single chat message
	 * 
	 * @return Number of bytes
	 */
	public int getSingleChatMessageMaxLength() {
		return maxMsgLengthSingleChat;
	}
	
	/**
	 * Return maximum length of a group chat message
	 * 
	 * @return Number of bytes
	 */
	public int getGroupChatMessageMaxLength() {
		return maxMsgLengthGroupChat;
	}
	
	/**
	 * Returns the max number of simultaneous group chats
	 * 
	 * @return Number
	 */
	public int getMaxGroupChats() {
		return maxGroupChat;
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
	 * Does auto accept mode activated for single chat
	 * 
	 * @return Boolean
	 */
	public boolean isChatAutoAcceptMode() {
		return autoAcceptSingleChat;
	}
	
	/**
	 * Does auto accept mode activated for group chat
	 * 
	 * @return Boolean
	 */
	public boolean isGroupChatAutoAcceptMode() {
		return autoAcceptGroupChat;
	}
	
	/**
	 * Does displayed delivery report activated on received chat messages
	 * 
	 * @return Boolean
	 */
	public boolean isDisplayedDeliveryReport() {
		return displayedDeliveryReport;
	}
	
	/**
	 * Activates or deactivates the displayed delivery report on received chat messages
	 * 
	 * @param state State
	 */
	public void setDisplayedDeliveryReport(boolean state) {
		this.displayedDeliveryReport = state;
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
