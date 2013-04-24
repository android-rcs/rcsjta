package com.orangelabs.rcs.service.api.client.messaging;

import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.service.api.client.messaging.GeolocMessage;

/**
 * Chat event listener
 */
interface IChatEventListener {
	// Session is started
	void handleSessionStarted();

	// Session has been aborted
	void handleSessionAborted(in int reason);
    
	// Session has been terminated by remote
	void handleSessionTerminatedByRemote();

	// New text message received
	void handleReceiveMessage(in InstantMessage msg);
	
	// Chat error
	void handleImError(in int error);

	// Is composing event
	void handleIsComposingEvent(in String contact, in boolean status);
	
	// Conference event
	void handleConferenceEvent(in String contact, in String contactDisplayname, in String state);

	// Message delivery status
	void handleMessageDeliveryStatus(in String msgId, in String status);

	// Request to add participant is successful
	void handleAddParticipantSuccessful();
    
	// Request to add participant has failed
	void handleAddParticipantFailed(in String reason);
	
	// New geoloc message received
	void handleReceiveGeoloc(in GeolocMessage geoloc);
}
