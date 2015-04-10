/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.richcall.video;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingError;
import com.gsma.rcs.core.ims.service.richcall.ContentSharingSession;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.IVideoPlayer;

/**
 * Video sharing streaming session
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class VideoStreamingSession extends ContentSharingSession {

    private int mOrientation;

    private int mWidth;

    private int mHeight;

    private IVideoPlayer mPlayer;

    private final static Logger sLogger = Logger.getLogger(VideoStreamingSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param content Content to be shared
     * @param contact Remote contact Id
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public VideoStreamingSession(ImsService parent, MmContent content, ContactId contact,
            RcsSettings rcsSettings, long timestamp, ContactsManager contactManager) {
        super(parent, content, contact, rcsSettings, timestamp, contactManager);
    }

    /**
     * Get the video orientation ID
     * 
     * @return Orientation
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * Set the video orientation ID
     * 
     * @param orientation
     */
    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    /**
     * Get the video width
     * 
     * @return Width
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Get the video height
     * 
     * @return Height
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Get the video player
     * 
     * @return Player
     */
    public IVideoPlayer getPlayer() {
        return mPlayer;
    }

    /**
     * Set the video player
     * 
     * @param player
     */
    public void setPlayer(IVideoPlayer player) {
        mPlayer = player;
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
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Session error: ").append(error.getErrorCode())
                    .append(", reason=").append(error.getMessage()).toString());
        }
        closeMediaSession();

        removeSession();

        ContactId contact = getRemoteContact();

        /* Request capabilities to the remote */
        getImsService().getImsModule().getCapabilityService().requestContactCapabilities(contact);

        for (ImsSessionListener imsSessionListener : getListeners()) {
            ((VideoStreamingSessionListener) imsSessionListener).handleSharingError(contact,
                    new ContentSharingError(error));
        }
    }

    @Override
    public void startSession() {
        getImsService().getImsModule().getRichcallService().addSession(this);
        start();
    }

    @Override
    public void removeSession() {
        getImsService().getImsModule().getRichcallService().removeSession(this);
    }
}
