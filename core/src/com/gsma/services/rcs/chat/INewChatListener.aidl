package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * New chat invitation event listener
 */
interface INewChatListener {
	void onNewSingleChat(in ContactId contact, in ChatMessage message);
	
	void onNewGroupChat(in String chatId);
}
