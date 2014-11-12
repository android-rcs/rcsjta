package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * One-to-One Chat interface
 */
interface IOneToOneChat {

	ContactId getRemoteContact();

	ChatMessage sendMessage(in String message);

	void sendIsComposingEvent(in boolean status);

	GeolocMessage sendMessage2(in Geoloc geoloc);
}