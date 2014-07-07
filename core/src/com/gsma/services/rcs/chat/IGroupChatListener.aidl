package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Group chat event listener
 */
interface IGroupChatListener {
	void onSessionStarted();
	
	void onSessionAborted();

	void onSessionError(in int reason);
		
	void onNewMessage(in ChatMessage message);

	void onNewGeoloc(in GeolocMessage message);

	void onReportMessageDelivered(in String msgId);

	void onReportMessageDisplayed(in String msgId);

	void onReportMessageFailed(in String msgId);
	
	void onComposingEvent(in ContactId contact, in boolean status);
	
	void onParticipantJoined(in ContactId contact, in String contactDisplayname);
	
	void onParticipantLeft(in ContactId contact);

	void onParticipantDisconnected(in ContactId contact);
	
	void onParticipantStatusChanged(in ParticipantInfo participantInfo);
}
