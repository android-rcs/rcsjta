package org.gsma.joyn.ipcall;

/**
 * IP call player event listener interface
 */
interface IIPCallPlayerListener {
	void onPlayerOpened();

	void onPlayerStarted();

	void onPlayerStopped();

	void onPlayerClosed();

	void onPlayerError(in int error);
}
