package org.gsma.joyn.chat;

import org.gsma.joyn.chat.IGroupChatListener;
import org.gsma.joyn.chat.Geoloc;
import org.gsma.joyn.ft.IFileTransfer;
import org.gsma.joyn.ft.IFileTransferListener;

/**
 * Group chat interface
 */
interface IGroupChat {
	String getChatId();

	int getState();	

	String getRemoteContact();

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

	String sendGeoloc(in Geoloc geoloc);

	IFileTransfer sendFile(in String filename, in String fileicon, in IFileTransferListener listener);
	
	int getServiceVersion();
}