package org.gsma.joyn.chat;

import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.GeolocMessage;

/**
 * Chat event listener
 */
interface IChatListener {
	void onNewMessage(in ChatMessage message);

	void onReportMessageDelivered(in String msgId);

	void onReportMessageDisplayed(in String msgId);

	void onReportMessageFailed(in String msgId);

	void onComposingEvent(in boolean status);
}