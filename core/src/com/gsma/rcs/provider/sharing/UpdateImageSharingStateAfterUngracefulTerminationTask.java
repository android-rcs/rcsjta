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

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.service.api.ImageSharingServiceImpl;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing.ReasonCode;
import com.gsma.services.rcs.sharing.image.ImageSharing.State;

import android.database.Cursor;

/* Update the state of ungracefully terminated Image sharing service object from
 * STARTED, INVITED and INITIATING to FAILED after rebooted. The reason of
 * ungracefully terminated could be sudden stack termination or battery drained,
 * which leads to wrong state in the database.
 * 
 * This class is used for error correction purpose, if no error was detected in
 * the database, means graceful termination is executed, so no update process will
 * be done.*/

public class UpdateImageSharingStateAfterUngracefulTerminationTask implements Runnable {

    private final RichCallHistory mRichCallHistory;

    private final ImageSharingServiceImpl mImageService;

    private static final Logger sLogger = Logger
            .getLogger(UpdateImageSharingStateAfterUngracefulTerminationTask.class.getName());

    public UpdateImageSharingStateAfterUngracefulTerminationTask(RichCallHistory rcHistory,
            ImageSharingServiceImpl imageService) {
        mRichCallHistory = rcHistory;
        mImageService = imageService;
    }

    public void run() {
        Cursor cursor = null;
        try {
            cursor = mRichCallHistory.getInterruptedImageSharings();
            /* TODO: Handle cursor when null. */
            int sharingIdx = cursor.getColumnIndexOrThrow(ImageSharingData.KEY_SHARING_ID);
            int contactIdx = cursor.getColumnIndexOrThrow(ImageSharingData.KEY_CONTACT);
            int stateIdx = cursor.getColumnIndexOrThrow(ImageSharingData.KEY_STATE);
            while (cursor.moveToNext()) {
                String sharingId = cursor.getString(sharingIdx);
                String contactNumber = cursor.getString(contactIdx);
                ContactId contact = ContactUtil.createContactIdFromTrustedData(contactNumber);
                State state = State.valueOf(cursor.getInt(stateIdx));
                switch (state) {
                    case STARTED:
                        mImageService.setImageSharingStateAndReasonCode(contact, sharingId,
                                State.FAILED, ReasonCode.FAILED_SHARING);
                        break;
                    case ACCEPTING:
                        mImageService.setImageSharingStateAndReasonCode(contact, sharingId,
                                State.ABORTED, ReasonCode.ABORTED_BY_SYSTEM);
                        break;
                    case INITIATING:
                        mImageService.setImageSharingStateAndReasonCode(contact, sharingId,
                                State.FAILED, ReasonCode.FAILED_INITIATION);
                        break;
                    case INVITED:
                        mImageService.setImageSharingStateAndReasonCode(contact, sharingId,
                                State.REJECTED, ReasonCode.REJECTED_BY_SYSTEM);
                        break;
                }
            }
        } catch (RuntimeException e) {
            /*
             * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
             * which should be handled/fixed within the code. However the cases when we are
             * executing operations on a thread unhandling such exceptions will eventually lead to
             * exit the system and thus can bring the whole system down, which is not intended.
             */
            sLogger.error(
                    "Exception occured while trying to update image sharing state for interrupted geoloc sharing",
                    e);
        } finally {
            CursorUtil.close(cursor);
        }
    }
}
