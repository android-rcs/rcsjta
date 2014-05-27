package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.chat.IChatListener;
import com.gsma.services.rcs.chat.Geoloc;

/**
 * Chat interface
 */
interface IChat {
	String getRemoteContact();
	
	String sendMessage(in String message);
	
	void sendIsComposingEvent(in boolean status);
	
	void addEventListener(in IChatListener listener);
	
	void removeEventListener(in IChatListener listener);

	String sendGeoloc(in Geoloc geoloc);
}