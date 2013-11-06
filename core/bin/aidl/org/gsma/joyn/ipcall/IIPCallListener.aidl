package org.gsma.joyn.ipcall;

/**
 * Callback methods for IP call events
 */
interface IIPCallListener {
	void onCallStarted();
	
	void onCallAborted();

	void onCallHeld();

	void onCallContinue();

	void onCallError(in int error);
}