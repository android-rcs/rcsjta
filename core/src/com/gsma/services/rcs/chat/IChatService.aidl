package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.chat.IChatListener;
import com.gsma.services.rcs.chat.IChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.INewChatListener;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Chat service API
 */
interface IChatService {
	boolean isServiceRegistered();
    
	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	ChatServiceConfiguration getConfiguration();
    
	IChat openSingleChat(in ContactId contact, in IChatListener listener);

	IGroupChat initiateGroupChat(in List<ContactId> contacts, in String subject, in IGroupChatListener listener);
    
	IGroupChat rejoinGroupChat(in String chatId);
    
	IGroupChat restartGroupChat(in String chatId);
    
	void addNewChatListener(in INewChatListener listener);
    
	void removeNewChatListener(in INewChatListener listener);
    
	IChat getChat(in ContactId contact);

	List<IBinder> getChats();

	List<IBinder> getGroupChats();
    
	IGroupChat getGroupChat(in String chatId);
	
	void markMessageAsRead(in String msgId);
	
	int getServiceVersion();
	
	void setRespondToDisplayReports(in boolean enable);
}