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

import com.gsma.services.rcs.vsh.IVideoPlayer;
import com.gsma.services.rcs.vsh.IVideoRenderer;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingError;
import com.orangelabs.rcs.core.ims.service.richcall.ContentSharingSession;
import com.orangelabs.rcs.core.ims.service.richcall.RichcallService;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Video sharing streaming session
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class VideoStreamingSession extends ContentSharingSession {
	/**
	 * Video width
	 */
	private int videoWidth = -1;
	
	/**
	 * Video height
	 */
	private int videoHeight = -1;

	/**
	 * Video renderer
	 */
	private IVideoRenderer renderer = null;

    /**
     * Video renderer
     */
    private IVideoPlayer player = null;

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
	 * Get the video width
	 * 
	 * @return Width
	 */
	public int getVideoWidth() {
		return videoWidth;
	}

	/**
	 * Get the video height
	 * 
	 * @return Height
	 */
	public int getVideoHeight() {
		return videoHeight;
	}

	/**
	 * Get the video renderer
	 * 
	 * @return Renderer
	 */
	public IVideoRenderer getVideoRenderer() {
		return renderer;
	}
	
	/**
	 * Set the video renderer
	 * 
	 * @param renderer Renderer
	 */
	public void setVideoRenderer(IVideoRenderer renderer) {
		this.renderer = renderer;
	}

    /**
     * Get the video player
     * 
     * @return Player
     */
    public IVideoPlayer getVideoPlayer() {
        return player;
    }

    /**
     * Set the video player
     *
     * @param Player
     */
    public void setVideoPlayer(IVideoPlayer player) {
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
        if (isSessionInterrupted()) {
            return;
        }

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
        for (int i = 0; i < getListeners().size(); i++) {
            ((VideoStreamingSessionListener) getListeners().get(i))
                    .handleSharingError(new ContentSharingError(error));
        }
    }
}
