package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.chat.IChatListener;
import com.gsma.services.rcs.chat.IChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Chat service API
 */
interface IChatService {

	boolean isServiceRegistered();
    
	void addServiceRegistrationListener(IRcsServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IRcsServiceRegistrationListener listener);

	ChatServiceConfiguration getConfiguration();
    
	IChat openSingleChat(in ContactId contact);

	IGroupChat initiateGroupChat(in List<ContactId> contacts, in String subject);
    
	IGroupChat rejoinGroupChat(in String chatId);
    
	IGroupChat restartGroupChat(in String chatId);
    
	IChat getChat(in ContactId contact);
    
	IGroupChat getGroupChat(in String chatId);
	
	void markMessageAsRead(in String msgId);

	void addGroupChatEventListener(in IGroupChatListener listener);

	void removeGroupChatEventListener(in IGroupChatListener listener);

	void addOneToOneChatEventListener(in IChatListener listener);

	void removeOneToOneChatEventListener(in IChatListener listener);

	int getServiceVersion();
	
	void setRespondToDisplayReports(in boolean enable);
}