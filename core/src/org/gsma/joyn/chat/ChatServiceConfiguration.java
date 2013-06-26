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
package org.gsma.joyn.chat;

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
	 * Max chat sessions
	 */
	private int maxChat; 

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
	 * Constructor
	 * 
	 * @param warnSF Store and Forward warning
	 * @param chatTimeout Chat timeout
	 * @param isComposingTimeout Is-composing timeout
	 * @param maxGroupChatParticipants Max participants in a group chat
	 * @param maxMsgLengthSingleChat Max length of a message in a single chat
	 * @param maxMsgLengthGroupChat Max length of a message in a group chat
	 * @param maxSingleChat Max single chats
	 * @param maxGroupChat Max group chats
	 * @param smsFallback SMS fallback
	 * @param autoAcceptSingleChat Auto accept mode for single chat
	 * @param autoAcceptGroupChat Auto accept mode for group chat
	 * @param displayedDeliveryReport Displayed delivery report
	 */
	public ChatServiceConfiguration(boolean warnSF, int chatTimeout, int isComposingTimeout,
			int maxGroupChatParticipants, int maxMsgLengthSingleChat, int maxMsgLengthGroupChat,
			int maxChat, boolean smsFallback, boolean autoAcceptSingleChat, boolean autoAcceptGroupChat,
			boolean displayedDeliveryReport) {
		this.warnSF = warnSF;
		this.chatTimeout = chatTimeout;
		this.isComposingTimeout = isComposingTimeout;
		this.maxGroupChatParticipants = maxGroupChatParticipants;
		this.maxMsgLengthSingleChat = maxMsgLengthSingleChat;
		this.maxMsgLengthGroupChat = maxMsgLengthGroupChat;
		this.maxChat = maxChat; 
		this.smsFallback = smsFallback;
		this.autoAcceptSingleChat = autoAcceptSingleChat;
		this.autoAcceptGroupChat = autoAcceptGroupChat;
		this.displayedDeliveryReport = displayedDeliveryReport;
    }	
	
	/**
	 * Constructor
	 * 
	 * @param source Parcelable source
	 */
	public ChatServiceConfiguration(Parcel source) {
		this.warnSF = source.readInt() != 0;
		this.chatTimeout = source.readInt();
		this.isComposingTimeout = source.readInt();
		this.maxGroupChatParticipants = source.readInt();
		this.maxMsgLengthSingleChat = source.readInt();
		this.maxMsgLengthGroupChat = source.readInt();
		this.maxChat = source.readInt(); 
		this.smsFallback = source.readInt() != 0;
		this.autoAcceptSingleChat = source.readInt() != 0;
		this.autoAcceptGroupChat = source.readInt() != 0;
		this.displayedDeliveryReport = source.readInt() != 0;
    }

	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
	 * 
	 * @return Integer
	 */
	public int describeContents() {
        return 0;
    }

	/**
	 * Write parcelable object
	 * 
	 * @param dest The Parcel in which the object should be written
	 * @param flags Additional flags about how the object should be written
	 */
    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeInt(warnSF ? 1 : 0);
    	dest.writeInt(chatTimeout);
    	dest.writeInt(isComposingTimeout);
    	dest.writeInt(maxGroupChatParticipants);
    	dest.writeInt(maxMsgLengthSingleChat);
    	dest.writeInt(maxMsgLengthGroupChat);
    	dest.writeInt(maxChat);
    	dest.writeInt(smsFallback ? 1 : 0);
    	dest.writeInt(autoAcceptSingleChat ? 1 : 0);
    	dest.writeInt(autoAcceptGroupChat ? 1 : 0);
    	dest.writeInt(displayedDeliveryReport ? 1 : 0);
    }

    /**
     * Parcelable creator
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
	 * the store and forward functionality is involved. It returns True if user should
	 * be informed when sending message to offline user.
	 * 
	 * @return Boolean
	 */
	public boolean isImWarnSF() {
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
	 * Returns maximum participants in group chat session
	 * 
	 * @return Number
	 */
	public int getGroupChatMaxParticipantsNumber() {
		return maxGroupChatParticipants;
	}
	
	/**
	 * Return maximum single chat message’s length can have
	 * 
	 * @return Number of bytes
	 */
	public int getSingleChatMessageMaxLength() {
		return maxMsgLengthSingleChat;
	}
	
	/**
	 * Returns the maximum single group chat message’s length can have
	 * 
	 * @return Number of bytes
	 */
	public int getGroupChatMessageMaxLength() {
		return maxMsgLengthGroupChat;
	}
	
	/**
	 * Returns the max number of simultaneous chat sessions
	 * 
	 * @return Number
	 */
	public int getMaxChatSessions() {
		return maxChat;
	}
	
	/**
	 * Does the UX proposes automatically a SMS fallback in case of chat initiation failure. It
	 * returns True if SMS fallback procedure is activated, else returns False.
	 * 
	 * @return Boolean
	 */
	public boolean isSmsFallback() {
		return smsFallback;
	}
	
	/**
	 * Returns True if auto accept mode activated for chat, else returns False
	 * 
	 * @return Boolean
	 */
	public boolean isChatAutoAcceptMode() {
		return autoAcceptSingleChat;
	}
	
	/**
	 * Returns True if auto accept mode activated for group chat, else returns False
	 * 
	 * @return Boolean
	 */
	public boolean isGroupChatAutoAcceptMode() {
		return autoAcceptGroupChat;
	}
	
	/**
	 * Is displayed delivery report on received chat messages activated
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
}
