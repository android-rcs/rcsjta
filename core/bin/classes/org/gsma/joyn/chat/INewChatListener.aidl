package org.gsma.joyn.chat;

import org.gsma.joyn.chat.ChatMessage;

/**
 * New chat invitation event listener
 */
interface INewChatListener {
	void onNewSingleChat(in String contact, in ChatMessage message);
	
	void onNewGroupChat(in String chatId);
}
