package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IFileTransferListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
 
/**
 * Group chat interface
 */
interface IGroupChat {
	String getChatId();

	int getDirection();
	
	int getState();	

	String getRemoteContact();

	String getSubject();	

	List<ParticipantInfo> getParticipants();
	
	void acceptInvitation();
	
	void rejectInvitation();
	
	String sendMessage(in String text);

	void sendIsComposingEvent(in boolean status);
	
	void sendDisplayedDeliveryReport(in String msgId);

	void addParticipants(in List<String> participants);
	
	int getMaxParticipants();
	
	void quitConversation();
	
	void addEventListener(in IGroupChatListener listener);
	
	void removeEventListener(in IGroupChatListener listener);

	String sendGeoloc(in Geoloc geoloc);
		
	IFileTransfer sendFile(in String filename, in boolean tryAttachThumbnail, in IFileTransferListener listener);
}