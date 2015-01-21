package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IOneToOneFileTransferListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Group chat interface
 */
interface IGroupChat {

	String getChatId();

	int getDirection();
	
	int getState();	

	int getReasonCode();

	ContactId getRemoteContact();

	String getSubject();	

	List<ParticipantInfo> getParticipants();

	IChatMessage sendMessage(in String text);

	void sendIsComposingEvent(in boolean status);

	void addParticipants(in List<ContactId> participants);
	
	int getMaxParticipants();
	
	void leave();

	IChatMessage sendMessage2(in Geoloc geoloc);

	void openChat();
}