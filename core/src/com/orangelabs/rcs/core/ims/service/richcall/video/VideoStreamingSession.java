/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.richcall.video;

import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingSession;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.service.api.client.media.IMediaEventListener;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.IMediaRenderer;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Video sharing streaming session
 * 
 * @author jexa7410
 */
public abstract class VideoStreamingSession extends ContentSharingSession {
    /**
	 * Media renderer
	 */
	private IMediaRenderer renderer = null;

    /**
     * Media renderer
     */
    private IMediaPlayer player = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param content Content to be shared
	 * @param contact Remote contact
	 */
	public VideoStreamingSession(ImsService parent, MmContent content, String contact) {
		super(parent, content, contact);
	}

	/**
	 * Get the media renderer
	 * 
	 * @return Renderer
	 */
	public IMediaRenderer getMediaRenderer() {
		return renderer;
	}
	
	/**
	 * Set the media renderer
	 * 
	 * @param renderer Renderer
	 */
	public void setMediaRenderer(IMediaRenderer renderer) {
		this.renderer = renderer;
	}

    /**
     * Get the media player
     * 
     * @return Player
     */
    public IMediaPlayer getMediaPlayer() {
        return player;
    }

    /**
     * Set the media player
     *
     * @param IMediaPlayer
     */
    public void setMediaPlayer(IMediaPlayer player) {
        this.player = player;
    }

	/**
	 * Receive BYE request 
	 * 
	 * @param bye BYE request
	 */
	public void receiveBye(SipRequest bye) {
		super.receiveBye(bye);
		
		// Request capabilities to the remote
		getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());
	}

    /**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
        return SipMessageFactory.createInvite(getDialogPath(),
                RichcallService.FEATURE_TAGS_VIDEO_SHARE, getDialogPath().getLocalContent());
    }

    /**
     * Handle error
     *
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        // Error
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());

        // Notify listeners
        if (!isInterrupted()) {
            for (int i = 0; i < getListeners().size(); i++) {
                ((VideoStreamingSessionListener) getListeners().get(i))
                        .handleSharingError(new ContentSharingError(error));
            }
        }
    }

    /**
     * Media player event listener
     */
    protected class MediaPlayerEventListener extends IMediaEventListener.Stub {
        /**
         * Streaming session
         */
        private VideoStreamingSession session;

        /**
         * Constructor
         *
         * @param session Streaming session
         */
        public MediaPlayerEventListener(VideoStreamingSession session) {
            this.session = session;
        }

        /**
         * Media player is opened
         */
        public void mediaOpened() {
            if (logger.isActivated()) {
                logger.debug("Media renderer is opened");
            }
        }

        /**
         * The size of media has changed
         *
         * @param width
         * @param height
         */
        public void mediaResized(int width, int height) {
            if (logger.isActivated()) {
                logger.debug("The size of media has changed " + width + "x" + height);
            }
            // Notify listeners
            if (!isInterrupted()) {
                for (int i = 0; i < getListeners().size(); i++) {
                    ((VideoStreamingSessionListener) getListeners().get(i))
                            .handleMediaResized(width, height);
                }
            }
        }

        /**
         * Media player is closed
         */
        public void mediaClosed() {
            if (logger.isActivated()) {
                logger.debug("Media renderer is closed");
            }
        }

        /**
         * Media player is started
         */
        public void mediaStarted() {
            if (logger.isActivated()) {
                logger.debug("Media renderer is started");
            }
        }

        /**
         * Media player is stopped
         */
        public void mediaStopped() {
            if (logger.isActivated()) {
                logger.debug("Media renderer is stopped");
            }
        }

        /**
         * Media player has failed
         *
         * @param error Error
         */
        public void mediaError(String error) {
            if (logger.isActivated()) {
                logger.error("Media renderer has failed: " + error);
            }

            // Close the media session
            closeMediaSession();

            // Terminate session
            terminateSession(ImsServiceSession.TERMINATION_BY_SYSTEM);

            // Remove the current session
            getImsService().removeSession(session);

            // Notify listeners
            if (!isInterrupted()) {
                for(int i=0; i < getListeners().size(); i++) {
                    ((VideoStreamingSessionListener)getListeners().get(i)).handleSharingError(new ContentSharingError(ContentSharingError.MEDIA_STREAMING_FAILED, error));
                }
            }

            // Request capabilities to the remote
            getImsService().getImsModule().getCapabilityService().requestContactCapabilities(getDialogPath().getRemoteParty());
        }
    }
}
