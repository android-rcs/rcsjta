package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Chat interface
 */
interface IChat {
	ContactId getRemoteContact();
	
	String sendMessage(in String message);
	
	void sendIsComposingEvent(in boolean status);

	String sendGeoloc(in Geoloc geoloc);
}