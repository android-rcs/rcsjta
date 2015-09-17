package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Group chat event listener
 */
interface IGroupChatListener {

	void onStateChanged(in String chatId, in int state, in int reasonCode);

	void onComposingEvent(String chatId, in ContactId contact, in boolean status);

	void onMessageStatusChanged(in String chatId, in String mimeType, in String msgId, in int status,
			 in int reasonCode);

	void onMessageGroupDeliveryInfoChanged(in String chatId, in ContactId contact, in String mimeType,
			in String msgId, in int status, in int reasonCode);

	void onParticipantStatusChanged(in String chatId, in ContactId contact, in int status);

	void onDeleted(in List<String> chatIds);

	void onMessagesDeleted(in String chatId, in List<String> msgIds);
}
