package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.ipcall.IIPCallPlayer;
import com.gsma.services.rcs.ipcall.IIPCallRenderer;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * IP call interface
 */
interface IIPCall {

	String getCallId();

	ContactId getRemoteContact();

	int getState();

	int getDirection();
	
	void acceptInvitation(IIPCallPlayer player, IIPCallRenderer renderer);

	void rejectInvitation();

	void abortCall();

	boolean isVideo();

	void addVideo();

	void removeVideo();

	boolean isOnHold();

	void holdCall();

	void continueCall();
}
