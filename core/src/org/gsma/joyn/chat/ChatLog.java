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

import android.net.Uri;

/**
 * Content provider for chat history
 *  
 * @author Jean-Marc AUFFRET
 */
public class ChatLog {
    /**
     * Content provider URI for chat conversations
     */
    public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/chat");

    /**
     * Chat
     */
    public static class Chat {
        /**
         * The name of the column containing the unique ID for a row.
         * <P>Type: primary key</P>
         */
        public static final String ID = "_id";

        /**
         * The name of the column containing the unique ID of the chat.
         * <P>Type: TEXT</P>
         */
        public static final String CHAT_ID = "chat_id";
        
        /**
         * The name of the column containing the type of chat.
         * <P>Type: INTEGER</P>
         */
        public static final String IS_GROUP_CHAT = "is_group";

        /**
         * The name of the column containing the state of the chat.
         * <P>Type: INTEGER</P>
         */
        public static final String STATE = "state";

        /**
         * The name of the column containing the identity of the remote party.
         * <P>Type: TEXT</P>
         */
        public static final String REMOTE = "remote";

        /**
         * The name of the column containing the subject in case of group chat.
         * <P>Type: TEXT</P>
         */
        public static final String SUBJECT = "subject";
        
        /**
         * The name of the column containing the time when chat is created.
         * <P>Type: LONG</P>
         */
        public static final String TIMESTAMP = "timestamp";
        
        /**
         * Type of chat
         */
        public static class Type {
	        /**
	         * Single chat
	         */
	        public static final int SINGLE_CHAT = 0;
	        
	        /**
	         * Group chat
	         */
	        public static final int GROUP_CHAT = 1;
        }
        
        /**
         * State of the chat
         */
        public static class State {
	        /**
	         * Chat is inactive (e.g. not yet started)
	         */
	        public static final int INACTIVE = 0;
        
	        /**
	         * Chat is active (e.g. started) 
	         */
	        public static final int ACTIVE = 1;

	        /**
	         * Chat has been terminated
	         */
	        public static final int TERMINATED = 2;

	        /**
	         * Chat has been terminated by the end user
	         */
	        public static final int TERMINATED_BY_USER = 3;

	        /**
	         * Chat has failed
	         */
	        public static final int FAILED = 4;

	        /**
	         * Chat invitation is pending
	         */
	        public static final int PENDING = 5;
        }        
    }

    /**
     * Chat message
     */
    public static class Message {
        /**
         * The name of the column containing the unique ID for a row.
         * <P>Type: primary key</P>
         */
        public static final String ID = "_id";

        /**
         * The name of the column containing the message ID.
         * <P>Type: TEXT</P>
         */
        public static final String MESSAGE_ID = "msg_id";
        
        /**
         * The name of the column containing the message status
         * <P>Type: INTEGER</P>
         */
        public static final String MESSAGE_STATUS = "status";
        	
        /**
         * The name of the column containing the message direction
         * <P>Type: INTEGER</P>
         */
        public static final String DIRECTION = "direction";

        /**
         * The name of the column containing the type of message
         * <P>Type: INTEGER</P>
         */
        public static final String MESSAGE_TYPE = "msg_type";

        /**
         * The name of the column containing the chat ID
         * <P>Type: TEXT</P>
         */
        public static final String CHAT_ID = "chat_id";

        /**
         * The name of the column containing the identity of the sender of the message.
         * <P>Type: TEXT</P>
         */
        public static final String CONTACT_NUMBER = "sender";
        
        /**
         * The name of the column containing the message body.
         * <P>Type: TEXT</P>
         */
        public static final String BODY = "body";
     
        /**
         * The name of the column containing the time when message is created.
         * <P>Type: LONG</P>
         */
        public static final String TIMESTAMP = "timestamp";
        
        /**
         * The name of the column containing the time when message is sent. If 0 means not sent.
         * <P>Type: LONG</P>
         */
        public static final String TIMESTAMP_SENT = "timestamp_sent";
        
        /**
         * The name of the column containing the time when message is delivered. If 0 means not delivered.
         * <P>Type: LONG</P>
         */
        public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";
        
        /**
         * The name of the column containing the time when message is delivered. If 0 means not delivered.
         * <P>Type: LONG</P>
         */
        public static final String TIMESTAMP_DISPLAYED = "timestamp_displayed";

        /**
         * The name of the column containing the MIME-TYPE of the message.
         * <P>Type: TEXT</P>
         */
        public static final String MIME_TYPE = "mime_type";
        
        /**
         * Type of the message
         */
        public static class Type {
	        /**
	         * Content message
	         */
	        public static final int CONTENT = 0;
	        
	        /**
	         * System message
	         */
	        public static final int SYSTEM = 1;
        }
        	
        /**
         * Direction of the message
         */
        public static class Direction {
	        /**
	         * Incoming message
	         */
	        public static final int INCOMING = 0;
	        
	        /**
	         * Outgoing message
	         */
	        public static final int OUTGOING = 1;
	        
	        /**
	         * Irrelevant or not applicable (e.g. for a system message)
	         */
	        public static final int IRRELEVANT = 2;
        }
        
        /**
         * Status of the message
         */
        public static class Status {
            /**
             * Status of a content message
             */
            public static class Content {
		        /**
		         * The message has been delivered, but we don't know if the message
		         * has been read by the remote
		         */
		        public static final int UNREAD = 0;
	
		        /**
		         * The message has been delivered and a displayed delivery report is
		         * requested, but we don't know if the message has been read by the remote
		         */
		        public static final int UNREAD_REPORT = 1;
		        
		        /**
		         * The message has been read by the remote (i.e. displayed)
		         */
		        public static final int READ = 2;
		        
		        /**
		         * The message is in progress of sending
		         */
		        public static final int SENDING = 3;
		        
		        /**
		         * The message has been sent
		         */
		        public static final int SENT = 4;
	
		        /**
		         * The message is failed to be sent
		         */
		        public static final int FAILED = 5;
		        
		        /**
		         * The message is queued to be sent by joyn service when possible
		         */
		        public static final int TO_SEND = 6;
		        
		        /**
		         * The message is a spam message
		         */
		        public static final int BLOCKED = 7;
            }
            
            /**
             * Status of the system message
             */
            public static class System {
		        /**
		         * Invitation of a participant is pending
		         */
		        public static final int INVITED = 0;
		        
		        /**
		         * Invitation is pending
		         */
		        public static final int PENDING = 1;

		        /**
		         * Invitation accepted by a participant
		         */
		        public static final int ACCEPTED = 2;
	        
		        /**
		         * Invitation declined by a participant
		         */
		        public static final int DECLINED = 3;
	        
		        /**
		         * Invitation of a participant has failed
		         */
		        public static final int FAILED = 4;
	        
		        /**
		         * Participant has joined the group chat
		         */
		        public static final int JOINED = 5;
	        
		        /**
		         * Participant has left the group chat (i.e. departed)
		         */
		        public static final int GONE = 6;

		        /**
		         * Participant has been disconnected from the group chat (i.e. booted)
		         */
		        public static final int DISCONNECT = 7;
		        
		        /**
		         * Participant is busy
		         */
		        public static final int BUSY = 8;
            }
        }
    }
}
