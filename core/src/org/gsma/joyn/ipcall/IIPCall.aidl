package org.gsma.joyn.ipcall;

import org.gsma.joyn.ipcall.IIPCallListener;
import org.gsma.joyn.ipcall.IIPCallPlayer;
import org.gsma.joyn.ipcall.IIPCallRenderer;

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

	void addVideo();

	void removeVideo();

	boolean isOnHold();

	void holdCall();

	void continueCall();
	
	void addEventListener(in IIPCallListener listener);

	void removeEventListener(in IIPCallListener listener);
	
	int getServiceVersion();
}
