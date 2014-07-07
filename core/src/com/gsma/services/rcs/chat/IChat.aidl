package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.IChatListener;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Chat interface
 */
interface IChat {
	ContactId getRemoteContact();
	
	String sendMessage(in String message);
	
	void sendIsComposingEvent(in boolean status);
	
	void addEventListener(in IChatListener listener);
	
	void removeEventListener(in IChatListener listener);

	String sendGeoloc(in Geoloc geoloc);
}