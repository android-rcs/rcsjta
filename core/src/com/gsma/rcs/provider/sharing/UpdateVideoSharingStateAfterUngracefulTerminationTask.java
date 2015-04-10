/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

import com.gsma.rcs.service.api.VideoSharingServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoSharing.ReasonCode;
import com.gsma.services.rcs.sharing.video.VideoSharing.State;

import android.database.Cursor;

/* Update the state of ungracefully terminated Video sharing service object from
 * STARTED, INVITED and INITIATING to FAILED after rebooted. The reason of
 * ungracefully terminated could be sudden stack termination or battery drained,
 * which leads to wrong state in the database.
 * 
 * This class is used for error correction purpose, if no error was detected in
 * the database, means graceful termination is executed, so no update process will
 * be done.*/

public class UpdateVideoSharingStateAfterUngracefulTerminationTask implements Runnable {

    private final RichCallHistory mRichCallHistory;

    private final VideoSharingServiceImpl mVideoService;

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    public UpdateVideoSharingStateAfterUngracefulTerminationTask(RichCallHistory rcHistory,
            VideoSharingServiceImpl videoService) {
        mRichCallHistory = rcHistory;
        mVideoService = videoService;
    }

    public void run() {
        Cursor cursor = null;
        try {
            cursor = mRichCallHistory.getInterruptedVideoSharings();
            int sharingIdx = cursor.getColumnIndexOrThrow(VideoSharingData.KEY_SHARING_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(VideoSharingData.KEY_CONTACT);
            int stateIdx = cursor.getColumnIndexOrThrow(VideoSharingData.KEY_STATE);
            while (cursor.moveToNext()) {
                String sharingId = cursor.getString(sharingIdx);
                String contactNumber = cursor.getString(contactIdx);
                ContactId contact = ContactUtil.createContactIdFromTrustedData(contactNumber);
                State state = State.valueOf(cursor.getInt(stateIdx));
                switch (state) {
                    case STARTED:
                        mVideoService.setVideoSharingStateAndReasonCode(contact, sharingId,
                                State.FAILED, ReasonCode.FAILED_SHARING);
                        break;
                    case INITIATING:
                        mVideoService.setVideoSharingStateAndReasonCode(contact, sharingId,
                                State.FAILED, ReasonCode.FAILED_INITIATION);
                        break;
                    case INVITED:
                        mVideoService.setVideoSharingStateAndReasonCode(contact, sharingId,
                                State.REJECTED, ReasonCode.REJECTED_BY_SYSTEM);
                        break;
                }
            }
        } catch (Exception e) {
            /*
             * Exception will be handled better in CR037.
             */
            if (mLogger.isActivated()) {
                mLogger.error(
                        "Exception occured while trying to update video sharing state for interrupted geoloc sharing",
                        e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
