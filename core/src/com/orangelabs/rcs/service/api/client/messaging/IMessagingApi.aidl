package com.orangelabs.rcs.service.api.client.messaging;

import com.orangelabs.rcs.service.api.client.messaging.IFileTransferSession;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;
import com.orangelabs.rcs.service.api.client.messaging.IMessageDeliveryListener;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;

/**
 * Messaging API
 */
interface IMessagingApi {
	// Transfer a file
	IFileTransferSession transferFile(in String contact, in String file, in boolean thumbnail);

	// Get current file transfer session from its session ID
	IFileTransferSession getFileTransferSession(in String id);

	// Get list of current file transfer sessions with a contact
	List<IBinder> getFileTransferSessionsWith(in String contact);

	// Get list of current file transfer sessions
	List<IBinder> getFileTransferSessions();

	// Initiate a one-to-one chat session
	IChatSession initiateOne2OneChatSession(in String contact, in String firstMsg);

	// Initiate an ad-hoc group chat session
	IChatSession initiateAdhocGroupChatSession(in List<String> participants, in String subject);

	// Rejoin a group chat session
	IChatSession rejoinGroupChatSession(in String chatId);

	// Restart a group chat session
	IChatSession restartGroupChatSession(in String chatId);

	// Get current chat session from its session ID
	IChatSession getChatSession(in String id);
	
	// Get list of current chat sessions with a contact
	List<IBinder> getChatSessionsWith(in String contact);

	// Get list of current chat sessions
	List<IBinder> getChatSessions();

	// Get list of current group chat sessions
	List<IBinder> getGroupChatSessions();

	// Get list of current group chat sessions for a given conversation
	List<IBinder> getGroupChatSessionsWith(in String chatId);

	// Set message delivery status
	void setMessageDeliveryStatus(in String contact, in String msgId, in String status);

	// Add message delivery listener
	void addMessageDeliveryListener(in IMessageDeliveryListener listener);

	// Remove message delivery listener
	void removeMessageDeliveryListener(in IMessageDeliveryListener listener);	
}
