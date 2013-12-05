package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.GeolocMessage;

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