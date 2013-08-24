package org.gsma.joyn.chat;

import org.gsma.joyn.chat.IChatListener;

/**
 * Chat interface
 */
interface IChat {
	String getChatId();
	
	String getRemoteContact();
	
	String sendMessage(in String message);
	
	void sendDisplayedDeliveryReport(in String msgId);
	
	void sendIsComposingEvent(in boolean status);
	
	void extendToGroup(in List<String> participants);
	
	void addEventListener(in IChatListener listener);
	
	void removeEventListener(in IChatListener listener);
}