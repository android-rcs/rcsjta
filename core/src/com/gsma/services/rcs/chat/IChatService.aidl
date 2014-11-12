package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.chat.IOneToOneChatListener;
import com.gsma.services.rcs.chat.IOneToOneChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Chat service API
 */
interface IChatService {

	boolean isServiceRegistered();
    
	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	ChatServiceConfiguration getConfiguration();
    
	IOneToOneChat openSingleChat(in ContactId contact);

	IGroupChat initiateGroupChat(in List<ContactId> contacts, in String subject);
    
	IGroupChat rejoinGroupChat(in String chatId);
    
	IGroupChat restartGroupChat(in String chatId);
    
	IOneToOneChat getChat(in ContactId contact);
    
	IGroupChat getGroupChat(in String chatId);
	
	void markMessageAsRead(in String msgId);

	void addEventListener3(in IGroupChatListener listener);

	void removeEventListener3(in IGroupChatListener listener);

	void addEventListener2(in IOneToOneChatListener listener);

	void removeEventListener2(in IOneToOneChatListener listener);

	int getServiceVersion();
	
	void setRespondToDisplayReports(in boolean enable);
}