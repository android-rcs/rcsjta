package org.gsma.joyn.chat;

import org.gsma.joyn.chat.IGroupChatListener;

/**
 * Group chat interface
 */
interface IGroupChat {
	String getChatId();

	String getSubject();

	List<String> getParticipants();
	
	void acceptInvitation();
	
	void rejectInvitation();
	
	String sendMessage(in String text);
	
	void sendIsComposingEvent(in boolean status);
	
	void addParticipants(in List<String> participants);
	
	int getMaxParticipants();
	
	void quitConversation();
	
	void addEventListener(in IGroupChatListener listener);
	
	void removeEventListener(in IGroupChatListener listener);
}