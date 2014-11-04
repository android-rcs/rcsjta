package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Group chat event listener
 */
interface IGroupChatListener {

	void onStateChanged(in String chatId, in int state, in int reasonCode);

	void onComposingEvent(String chatId, in ContactId contact, in boolean status);

	void onMessageStatusChanged(in String chatId, in String msgId, in int status, in int reasonCode);

	void onMessageGroupDeliveryInfoChanged(in String chatId, in ContactId contact, in String msgId, in int status, in int reasonCode);

	void onParticipantInfoChanged(in String chatId, in ParticipantInfo info);
}
