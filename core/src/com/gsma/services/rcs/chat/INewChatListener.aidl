package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatMessage;

/**
 * New chat invitation event listener
 */
interface INewChatListener {
	void onNewSingleChat(in String contact, in ChatMessage message);
	
	void onNewGroupChat(in String chatId);
}
