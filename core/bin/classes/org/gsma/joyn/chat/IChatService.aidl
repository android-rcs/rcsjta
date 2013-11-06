package org.gsma.joyn.chat;

import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.chat.IChatListener;
import org.gsma.joyn.chat.IChat;
import org.gsma.joyn.chat.IGroupChatListener;
import org.gsma.joyn.chat.IGroupChat;
import org.gsma.joyn.chat.INewChatListener;
import org.gsma.joyn.chat.ChatServiceConfiguration;

/**
 * Chat service API
 */
interface IChatService {
	boolean isServiceRegistered();
    
	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	ChatServiceConfiguration getConfiguration();
    
	IChat openSingleChat(in String contact, in IChatListener listener);

	IGroupChat initiateGroupChat(in List<String> contacts, in String subject, in IGroupChatListener listener);
    
	IGroupChat rejoinGroupChat(in String chatId);
    
	IGroupChat restartGroupChat(in String chatId);
    
	void addNewChatListener(in INewChatListener listener);
    
	void removeNewChatListener(in INewChatListener listener);
    
	IChat getChat(in String contact);

	List<IBinder> getChats();

	List<IBinder> getGroupChats();
    
	IGroupChat getGroupChat(in String chatId);
	
	int getServiceVersion();
}