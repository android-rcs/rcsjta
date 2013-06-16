package org.gsma.joyn.chat;

import org.gsma.joyn.chat.ChatMessage;

/**
 * Group chat event listener
 */
interface IGroupChatListener {
	void onSessionStarted();
	
	void onNewMessage(in ChatMessage message);
	
	void onReportMessageDelivered(in String msgId);

	void onReportMessageDisplayed(in String msgId);

	void onReportMessageFailed(in String msgId);
	
	void onComposingEvent(in String contact, in boolean status);
	
	void onSessionTerminated(in int reason);

	void onSessionError(in int reason);
	
	void onParticipantJoined(in String contact, in String contactDisplayname);
	
	void onParticipantLeft(in String contact);

	void onParticipantDisconnected(in String contact);
}
