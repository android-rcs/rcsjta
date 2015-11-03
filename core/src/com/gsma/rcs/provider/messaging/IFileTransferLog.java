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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;
import android.net.Uri;

import java.util.List;
import java.util.Set;

/**
 * Interface for the ft table
 * 
 * @author LEMORDANT Philippe
 */
public interface IFileTransferLog {

    /**
     * Add outgoing one to one file transfer
     * 
     * @param fileTransferId File Transfer ID
     * @param contact Contact ID
     * @param direction Direction
     * @param content File content
     * @param fileIcon File icon content
     * @param state File transfer state
     * @param reasonCode Reason code
     * @param timestamp Local timestamp for both incoming and outgoing file transfer for one-one
     *            chat
     * @param timestampSent Timestamp sent in payload for both incoming and outgoing file transfer
     *            for one-one chat
     * @param fileExpiration the time when file on the content server is no longer valid to
     *            download.
     * @param fileIconExpiration the time when file icon on the content server is no longer valid to
     *            download.
     */
    void addOneToOneFileTransfer(String fileTransferId, ContactId contact, Direction direction,
            MmContent content, MmContent fileIcon, State state, ReasonCode reasonCode,
            long timestamp, long timestampSent, long fileExpiration, long fileIconExpiration);

    /**
     * Add an outgoing File Transfer supported by Group Chat
     * 
     * @param fileTransferId the identity of the file transfer
     * @param chatId the identity of the group chat
     * @param content the File content
     * @param fileIcon the fileIcon content
     * @param state File transfer state
     * @param reasonCode Reason code
     * @param timestamp Local timestamp for outgoing file transfer for a group chat
     * @param timestampSent Timestamp sent in payload for outgoing file transfer for a group chat
     */
    void addOutgoingGroupFileTransfer(String fileTransferId, String chatId, MmContent content,
            MmContent fileIcon, Set<ContactId> recipients, State state, ReasonCode reasonCode,
            long timestamp, long timestampSent);

    /**
     * Add incoming group file transfer
     * 
     * @param fileTransferId File transfer ID
     * @param chatId Chat ID
     * @param contact Contact ID
     * @param content File content
     * @param fileIcon File icon contentID
     * @param state File transfer state
     * @param reasonCode Reason code
     * @param timestamp Local timestamp for incoming file transfer for a group chat
     * @param timestampSent Timestamp sent in payload for incoming file transfer for a group chat
     * @param fileExpiration the time when file on the content server is no longer valid to
     *            download.
     * @param fileIconExpiration the time when file icon on the content server is no longer valid to
     *            download.
     */
    void addIncomingGroupFileTransfer(String fileTransferId, String chatId, ContactId contact,
            MmContent content, MmContent fileIcon, State state, ReasonCode reasonCode,
            long timestamp, long timestampSent, long fileExpiration, long fileIconExpiration);

    /**
     * Set file transfer state and reason code. Note that this method should not be used for
     * State.DELIVERED and State.DISPLAYED. These states require timestamps and should be set
     * through setFileTransferDelivered and setFileTransferDisplayed respectively.
     * 
     * @param fileTransferId File transfer ID
     * @param state File transfer state (see restriction above)
     * @param reasonCode File transfer state reason code
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileTransferStateAndReasonCode(String fileTransferId, State state,
            ReasonCode reasonCode);

    /**
     * Set file transfer state, reason code, timestamp and timestampSent
     * 
     * @param fileTransferId File transfer ID
     * @param state New file transfer state
     * @param reasonCode New file transfer reason code
     * @param timestamp New local timestamp for the file transfer
     * @param timestampSent New timestamp sent in payload for the file transfer
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileTransferStateAndTimestamp(String fileTransferId, State state,
            ReasonCode reasonCode, long timestamp, long timestampSent);

    /**
     * Update file transfer read status
     * 
     * @param fileTransferId File transfer ID
     */
    void markFileTransferAsRead(String fileTransferId);

    /**
     * Update file transfer download progress
     * 
     * @param fileTransferId File transfer ID
     * @param currentSize Current size
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileTransferProgress(String fileTransferId, long currentSize);

    /**
     * Set file transfer URI
     * 
     * @param fileTransferId File transfer ID
     * @param content the MmContent of received file
     * @param fileExpiration the time when file on the content server is no longer valid to download
     * @param fileIconExpiration the time when file icon on the content server is no longer valid to
     *            download
     * @param deliveryExpiration delivery expiration
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileTransferred(String fileTransferId, MmContent content, long fileExpiration,
            long fileIconExpiration, long deliveryExpiration);

    /**
     * Tells if the MessageID corresponds to that of a file transfer
     * 
     * @param fileTransferId File Transfer Id
     * @return boolean If there is File Transfer corresponding to msgId
     */
    boolean isFileTransfer(String fileTransferId);

    /**
     * Returns the icon for a file transfer
     * 
     * @param fileTransferId the file transfer ID
     * @return the icon or null if there s none
     */
    String getFileTransferIcon(String fileTransferId);

    /**
     * Set file upload TID
     * 
     * @param fileTransferId File transfer ID
     * @param tId TID
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileUploadTId(String fileTransferId, String tId);

    /**
     * Set file download server uri
     * 
     * @param fileTransferId File transfer ID
     * @param downloadAddress Download Address
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileDownloadAddress(String fileTransferId, Uri downloadAddress);

    /**
     * Retrieve file transfers paused by SYSTEM on connection loss
     * 
     * @return list of FtHttpResume
     */
    List<FtHttpResume> retrieveFileTransfersPausedBySystem();

    /**
     * Retrieve resumable file upload
     * 
     * @param tId Unique Id used while uploading
     * @return instance of FtHttpResumeUpload
     */
    FtHttpResumeUpload retrieveFtHttpResumeUpload(String tId);

    /**
     * Get the chat id for a file transfer with specific id
     * 
     * @param fileTransferId specific id
     * @return chat id
     */
    String getFileTransferChatId(String fileTransferId);

    /**
     * Get file transfer state from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return State
     */
    State getFileTransferState(String fileTransferId);

    /**
     * Get file transfer reason code from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return reason code on the file transfer
     */
    ReasonCode getFileTransferReasonCode(String fileTransferId);

    /**
     * Get file transfer data from its unique ID
     * 
     * @param fileTransferId the file transfer ID
     * @return Cursor or null if no data exists
     */
    Cursor getFileTransferData(String fileTransferId);

    /**
     * Is group file transfer
     * 
     * @param fileTransferId the file transfer ID
     * @return true if it is group file transfer
     */
    boolean isGroupFileTransfer(String fileTransferId);

    /**
     * Get file transfer timestamp from file transfer Id
     * 
     * @param fileTransferId the file transfer ID
     * @return timestamp
     */
    Long getFileTransferTimestamp(String fileTransferId);

    /**
     * Get file transfer sent timestamp from file transfer Id
     * 
     * @param fileTransferId the file transfer ID
     * @return sent timestamp
     */
    Long getFileTransferSentTimestamp(String fileTransferId);

    /**
     * Get file transfer resume info from its corresponding filetransferId
     * 
     * @param fileTransferId the file transfer ID
     * @return FtHttpResume
     */
    FtHttpResume getFileTransferResumeInfo(String fileTransferId);

    /**
     * Get all one-to-one and group file transfers that are queued and uploaded but not transferred
     * in ascending order of timestamp
     * 
     * @return Cursor
     */
    Cursor getQueuedAndUploadedButNotTransferredFileTransfers();

    /**
     * Get interrupted file transfers
     * 
     * @return
     */
    Cursor getInterruptedFileTransfers();

    /**
     * Sets remote SIP Instance identifier for download HTTP file transfer
     * 
     * @param fileTransferId the file transfer ID
     * @param remoteInstanceId the remote instance ID
     * @return True if an entry was updated, otherwise false
     */
    boolean setRemoteSipId(String fileTransferId, String remoteInstanceId);

    /**
     * Set file transfer delivered
     * 
     * @param fileTransferId File transfer ID
     * @param timestampDelivered Time delivered
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileTransferDelivered(String fileTransferId, long timestampDelivered);

    /**
     * Set file transfer displayed
     * 
     * @param fileTransferId File transfer ID
     * @param timestampDisplayed Time displayed
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileTransferDisplayed(String fileTransferId, long timestampDisplayed);

    /**
     * Marks undelivered file transfers to indicate that transfers have been processed.
     * 
     * @param fileTransferIds the file transfer IDs
     */
    void clearFileTransferDeliveryExpiration(List<String> fileTransferIds);

    /**
     * Set file transfer delivery expired for specified file transfer id.
     * 
     * @param fileTransferId the file transfer ID
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileTransferDeliveryExpired(String fileTransferId);

    /**
     * Get one-one file transfers with unexpired delivery
     * 
     * @return Cursor
     */
    Cursor getUnDeliveredOneToOneFileTransfers();

    /**
     * Returns true if delivery for this file has expired or false otherwise. Note: false means
     * either that delivery for this file has not yet expired, delivery has been successful,
     * delivery expiration has been cleared (see clearFileTransferDeliveryExpiration) or that this
     * particular file is not eligible for delivery expiration in the first place.
     * 
     * @param fileTransferId the file transfer ID
     * @return boolean
     */
    Boolean isFileTransferExpiredDelivery(String fileTransferId);

    /**
     * Set file transfer download info in DB
     * 
     * @param fileTransferId the file transfer ID
     * @param ftHttpInfo the file transfer HTTP information
     */
    void setFileTransferDownloadInfo(String fileTransferId, FileTransferHttpInfoDocument ftHttpInfo);

    /**
     * Get file download info
     * 
     * @param fileTransferId the file transfer ID
     * @return FileTransferHttpInfoDocument
     * @throws FileAccessException
     */
    FileTransferHttpInfoDocument getFileDownloadInfo(String fileTransferId)
            throws FileAccessException;

    /**
     * Get file download info
     * 
     * @param cursor the cursor
     * @return FileTransferHttpInfoDocument
     * @throws FileAccessException
     */
    FileTransferHttpInfoDocument getFileDownloadInfo(Cursor cursor) throws FileAccessException;

    /**
     * Set file transfer timestamp and timestampSent
     * 
     * @param fileTransferId File transfer ID
     * @param timestamp New local timestamp for the file transfer
     * @param timestampSent New timestamp sent in payload for the file transfer
     */
    void setFileTransferTimestamps(String fileTransferId, long timestamp, long timestampSent);

    /**
     * Set file info dequeued successfully.
     * 
     * @param fileTransferId the file transfer ID
     * @param deliveryExpiration the delivery expiration
     * @return True if an entry was updated, otherwise false
     */
    boolean setFileInfoDequeued(String fileTransferId, long deliveryExpiration);

    /**
     * Gets the number of transferred bytes
     * 
     * @param fileTransferId the file transfer ID
     * @return the number of transferred bytes
     */
    Long getFileTransferProgress(String fileTransferId);
}
