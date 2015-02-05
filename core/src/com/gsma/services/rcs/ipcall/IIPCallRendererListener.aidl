package com.gsma.services.rcs.ipcall;

/**
 * IP call renderer event listener interface
 */
interface IIPCallRendererListener {
	void onRendererOpened();

	void onRendererStarted();

	void onRendererStopped();

	void onRendererClosed();

	void onRendererError(in int error);
}
