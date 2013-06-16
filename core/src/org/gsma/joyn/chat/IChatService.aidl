package org.gsma.joyn.chat;

import org.gsma.joyn.chat.IChatListener;
import org.gsma.joyn.chat.IChat;
import org.gsma.joyn.chat.IGroupChatListener;
import org.gsma.joyn.chat.IGroupChat;
import org.gsma.joyn.chat.INewChatListener;

/**
 * Chat service API
 */
interface IChatService {
	IChat initiateSingleChat(in String contact, in String firstMessage, in IChatListener listener);
    
    IGroupChat initiateGroupChat(in List<String> contacts, in String subject, in IGroupChatListener listener);
    
    IGroupChat rejoinGroupChat(in String chatId);
    
    IGroupChat restartGroupChat(in String chatId);
    
    void deleteChat(in String chatId);
    
    void addEventListener(in INewChatListener listener);
    
    void removeEventListener(in INewChatListener listener);
    
    List<IBinder> getChats();
    
    IChat getChat(in String chatId);

    List<IBinder> getGroupChats();
    
    IGroupChat getGroupChat(in String chatId);
}