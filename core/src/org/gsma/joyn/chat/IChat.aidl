package org.gsma.joyn.chat;

import org.gsma.joyn.chat.IChatListener;
import org.gsma.joyn.chat.Geoloc;

/**
 * Chat interface
 */
interface IChat {
	String getRemoteContact();
	
	String sendMessage(in String message);
	
	void sendDisplayedDeliveryReport(in String msgId);
	
	void sendIsComposingEvent(in boolean status);
	
	void addEventListener(in IChatListener listener);
	
	void removeEventListener(in IChatListener listener);

	String sendGeoloc(in Geoloc geoloc);
	
	int getServiceVersion();
}