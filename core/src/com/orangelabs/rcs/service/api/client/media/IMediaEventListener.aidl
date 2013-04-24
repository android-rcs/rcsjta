package com.orangelabs.rcs.service.api.client.media;

/**
 * Media event listener
 */
interface IMediaEventListener {
	// Media is opened
	void mediaOpened();

    // The size of media has changed
    void mediaResized(in int width, in int height);

	// Media is closed
	void mediaClosed();

	// Media is started
	void mediaStarted();
	
	// Media is stopped
	void mediaStopped();

	// Media has failed
	void mediaError(in String error);
}
