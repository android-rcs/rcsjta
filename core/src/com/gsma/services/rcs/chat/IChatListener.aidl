package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Chat event listener
 */
interface IChatListener {

	void onMessageStatusChanged(in ContactId contact, in String msgId, in int status, in int reasonCode);

	void onComposingEvent(in ContactId contact, in boolean status);
}