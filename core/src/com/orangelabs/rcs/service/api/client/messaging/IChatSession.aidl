package com.orangelabs.rcs.service.api.client.messaging;

import com.orangelabs.rcs.service.api.client.messaging.IChatEventListener;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;

/**
 * Chat session interface
 */
interface IChatSession {
	// Get session ID
	String getSessionID();

	// Get chat ID
	String getChatID();

	// Get remote contact
	String getRemoteContact();
	
	// Get session state
	int getSessionState();

	// Is group chat
	boolean isGroupChat();
	
	// Is Store & Forward
	boolean isStoreAndForward();

	// Get first message exchanged during the session
	InstantMessage getFirstMessage();

	// Get subject associated to the session
	String getSubject();

	// Accept the session invitation
	void acceptSession();

	// Reject the session invitation
	void rejectSession();

	// Cancel the session
	void cancelSession();

	// Get list of participants in the session
	List<String> getParticipants();

	// Get max number of participants in the session
	int getMaxParticipants();

	// Get max number of participants which can be added to the conference
	int getMaxParticipantsToBeAdded();

	// Add a participant to the session
	void addParticipant(in String participant);

	// Add a list of participants to the session
	void addParticipants(in List<String> participants);

	// Send a text message
	String sendMessage(in String text);

	// Is geoloc supported
	boolean isGeolocSupported();

	// Send a geoloc message
	String sendGeoloc(in GeolocPush geoloc);

	// Is file transfer over HTTP supported
	boolean isFileTransferHttpSupported();

	// Send file over HTTP
	void sendFile(in String filename);

	// Set is composing status
	void setIsComposingStatus(in boolean status);

	// Set message delivery status
	void setMessageDeliveryStatus(in String msgId, in String status);

	// Add session listener
	void addSessionListener(in IChatEventListener listener);

	// Remove session listener
	void removeSessionListener(in IChatEventListener listener);	
}
