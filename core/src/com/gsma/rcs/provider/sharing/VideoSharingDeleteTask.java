/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.provider.sharing;

import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.video.VideoStreamingSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.VideoSharingServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

public class VideoSharingDeleteTask extends DeleteTask.GroupedByContactId {

    private static final Logger sLogger = Logger.getLogger(VideoSharingDeleteTask.class.getName());

    private final VideoSharingServiceImpl mVideoSharingService;

    private final RichcallService mRichcallService;

    /**
     * Deletion of all video sharing .
     * 
     * @param videoSharingService the video sharing service impl
     * @param richcallService the rich call service
     * @param contentResolver the local content resolver
     * @param imsLock the ims operation lock
     */
    public VideoSharingDeleteTask(VideoSharingServiceImpl videoSharingService,
            RichcallService richcallService, LocalContentResolver contentResolver) {
        super(contentResolver, VideoSharingData.CONTENT_URI, VideoSharingData.KEY_SHARING_ID,
                VideoSharingData.KEY_CONTACT, (String) null);
        mVideoSharingService = videoSharingService;
        mRichcallService = richcallService;
    }

    /**
     * Deletion of a specific video sharing.
     * 
     * @param videoSharingService the video sharing service impl
     * @param richcallService the rich call service
     * @param contentResolver the local content resolver
     * @param imsLock the ims operation lock
     * @param sharingId the sharing id
     */
    public VideoSharingDeleteTask(VideoSharingServiceImpl videoSharingService,
            RichcallService richcallService, LocalContentResolver contentResolver,
            String sharingId) {
        super(contentResolver, VideoSharingData.CONTENT_URI, VideoSharingData.KEY_SHARING_ID,
                VideoSharingData.KEY_CONTACT, null, sharingId);
        mVideoSharingService = videoSharingService;
        mRichcallService = richcallService;
    }

    /**
     * Deletion of all video sharing with a specific contact.
     *
     * @param videoSharingService the video sharing service impl
     * @param richcallService the rich call service
     * @param contentResolver the local content resolver
     * @param imsLock the ims operation lock
     * @param contact the contact id
     */
    public VideoSharingDeleteTask(VideoSharingServiceImpl videoSharingService,
            RichcallService richcallService, LocalContentResolver contentResolver,
            ContactId contact) {
        super(contentResolver, VideoSharingData.CONTENT_URI,
                VideoSharingData.KEY_SHARING_ID, VideoSharingData.KEY_CONTACT, contact);
        mVideoSharingService = videoSharingService;
        mRichcallService = richcallService;
    }

    @Override
    protected void onRowDelete(ContactId contact, String sharingId) throws SipPayloadException {
        VideoStreamingSession session = mRichcallService.getVideoSharingSession(sharingId);
        if (session == null) {
            mVideoSharingService.removeVideoSharing(sharingId);
            return;

        }
        try {
            session.deleteSession();
        } catch (SipNetworkException e) {
            /*
             * If network is lost during a delete operation the remaining part of the delete
             * operation (delete from persistent storage) can succeed to 100% anyway since delete
             * can be executed anyway while no network connectivity is present and still succeed.
             */
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
        }
        mVideoSharingService.removeVideoSharing(sharingId);
    }

    @Override
    protected void onCompleted(ContactId contact, Set<String> deletedIds) {
        mVideoSharingService.broadcastDeleted(contact, deletedIds);
    }
}
