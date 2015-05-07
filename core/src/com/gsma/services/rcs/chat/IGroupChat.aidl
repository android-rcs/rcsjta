package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.IChatMessage;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.filetransfer.IFileTransfer;
import com.gsma.services.rcs.filetransfer.IOneToOneFileTransferListener;
import com.gsma.services.rcs.contact.ContactId;

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

	Map getParticipants();

	long getTimestamp();

	IChatMessage sendMessage(in String text);

	void setComposingStatus(in boolean ongoing);

	void inviteParticipants(in List<ContactId> participants);
	
	int getMaxParticipants();
	
	void leave();

	IChatMessage sendMessage2(in Geoloc geoloc);

	void openChat();

	boolean isAllowedToSendMessage();

	boolean isAllowedToInviteParticipants();

	boolean isAllowedToInviteParticipant(in ContactId participant);

	boolean isAllowedToLeave();
}