package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.chat.IOneToOneChatListener;
import com.gsma.services.rcs.chat.IOneToOneChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IChatServiceConfiguration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * Chat service API
 */
interface IChatService {

	boolean isServiceRegistered();
	
	int getServiceRegistrationReasonCode();
    
	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	IChatServiceConfiguration getConfiguration();
    
	IGroupChat initiateGroupChat(in List<ContactId> contacts, in String subject);

	IOneToOneChat getOneToOneChat(in ContactId contact);

	IGroupChat getGroupChat(in String chatId);
	
	void markMessageAsRead(in String msgId);

	void addEventListener3(in IGroupChatListener listener);

	void removeEventListener3(in IGroupChatListener listener);

	void addEventListener2(in IOneToOneChatListener listener);

	void removeEventListener2(in IOneToOneChatListener listener);

	int getServiceVersion();
	
	void setRespondToDisplayReports(in boolean enable);

	IChatMessage getChatMessage(in String msgId);
	
	ICommonServiceConfiguration getCommonConfiguration();

	boolean isAllowedToInitiateGroupChat();

	boolean isAllowedToInitiateGroupChat2(in ContactId contact);

	void deleteOneToOneChats();

	void deleteGroupChats();

	void deleteOneToOneChat(in ContactId contact);

	void deleteGroupChat(in String chatId);

	void deleteMessage(in String msgId);

	void markUndeliveredMessagesAsProcessed(in List<String> msgIds);
}