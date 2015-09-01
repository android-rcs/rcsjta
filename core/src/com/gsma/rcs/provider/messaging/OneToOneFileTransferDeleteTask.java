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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.FileTransferServiceImpl;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

public class OneToOneFileTransferDeleteTask extends DeleteTask.GroupedByContactId {

    private static final Logger sLogger = Logger.getLogger(OneToOneFileTransferDeleteTask.class
            .getName());

    private static final String SELECTION_ALL_ONETOONE_FILETRANSFERS = new StringBuilder(
            FileTransferData.KEY_CHAT_ID).append("=").append(FileTransferData.KEY_CONTACT)
            .toString();

    private final FileTransferServiceImpl mFileTransferService;

    private final InstantMessagingService mImService;

    /**
     * Deletion of all one to one file transfers.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the IMS operation lock
     */
    public OneToOneFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver, Object imsLock) {
        super(contentResolver, imsLock, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CONTACT, SELECTION_ALL_ONETOONE_FILETRANSFERS);
        mFileTransferService = fileTransferService;
        mImService = imService;
    }

    /**
     * Deletion of a specific file transfer.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the IMS operation lock
     * @param transferId the transfer id
     */
    public OneToOneFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            Object imsLock, String transferId) {
        super(contentResolver, imsLock, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CONTACT, null, transferId);
        mFileTransferService = fileTransferService;
        mImService = imService;
    }

    /**
     * Deletion of all file transfers from a specific one to one conversation.
     * 
     * @param fileTransferService the file transfer service impl
     * @param imService the IM service
     * @param contentResolver the content resolver
     * @param imsLock the IMS operation lock
     * @param contact the contact id
     */
    public OneToOneFileTransferDeleteTask(FileTransferServiceImpl fileTransferService,
            InstantMessagingService imService, LocalContentResolver contentResolver,
            Object imsLock, ContactId contact) {
        super(contentResolver, imsLock, FileTransferData.CONTENT_URI, FileTransferData.KEY_FT_ID,
                FileTransferData.KEY_CONTACT, contact);
        mFileTransferService = fileTransferService;
        mImService = imService;
    }

    @Override
    protected void onRowDelete(ContactId contact, String transferId) throws SipPayloadException {
        FileSharingSession session = mImService.getFileSharingSession(transferId);
        if (session == null) {
            mFileTransferService.ensureThumbnailIsDeleted(transferId);
            mFileTransferService.removeOneToOneFileTransfer(transferId);
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
        mFileTransferService.removeOneToOneFileTransfer(transferId);
    }

    @Override
    protected void onCompleted(ContactId contact, Set<String> transferIds) {
        mFileTransferService.broadcastOneToOneFileTransferDeleted(contact, transferIds);
    }
}
