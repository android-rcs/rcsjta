package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IFileTransferListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Group chat interface
 */
interface IGroupChat {

	String getChatId();

	int getDirection();
	
	int getState();	

	ContactId getRemoteContact();

	String getSubject();	

	List<ParticipantInfo> getParticipants();
	
	void acceptInvitation();
	
	void rejectInvitation();
	
	String sendMessage(in String text);

	void sendIsComposingEvent(in boolean status);

	void addParticipants(in List<ContactId> participants);
	
	int getMaxParticipants();
	
	void quitConversation();

	String sendGeoloc(in Geoloc geoloc);
}