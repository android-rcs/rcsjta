package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.ipcall.IIPCallListener;
import com.gsma.services.rcs.ipcall.IIPCallPlayer;
import com.gsma.services.rcs.ipcall.IIPCallRenderer;

/**
 * IP call interface
 */
interface IIPCall {

	String getCallId();

	String getRemoteContact();

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
	
	void addEventListener(in IIPCallListener listener);

	void removeEventListener(in IIPCallListener listener);
}
