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

package com.gsma.services.rcs.filetransfer;

import com.gsma.services.rcs.RcsService.Direction;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Content provider for file transfer history
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferLog {
    /**
     * Content provider URI
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.filetransfer/filetransfer");

    /**
     * The name of the column containing the unique id across provider tables.
     * <P>
     * Type: INTEGER
     * </P>
     */
    public static final String BASECOLUMN_ID = BaseColumns._ID;

    /**
     * The name of the column containing the unique ID of the file transfer.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String FT_ID = "ft_id";

    /**
     * The name of the column containing the identity of the remote contact.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String CONTACT = "contact";

    /**
     * The name of the column containing the URI of the file.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String FILE = "file";

    /**
     * The name of the column containing the filename (absolute path).
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String FILENAME = "filename";

    /**
     * The name of the column containing the file size to be transferred (in bytes).
     * <P>
     * Type: LONG
     * </P>
     */
    public static final String FILESIZE = "filesize";

    /**
     * The name of the column containing the MIME-type of the file.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String MIME_TYPE = "mime_type";

    /**
     * The name of the column containing the direction of the transfer.
     * <P>
     * Type: INTEGER
     * </P>
     * 
     * @see Direction
     */
    public static final String DIRECTION = "direction";

    /**
     * The name of the column containing the amount of data transferred (in bytes).
     * <P>
     * Type: LONG
     * </P>
     */
    public static final String TRANSFERRED = "transferred";

    /**
     * The name of the column containing the time when transfer is initiated.
     * <P>
     * Type: LONG
     * </P>
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * The name of the column containing the time when file is sent. If 0 means not sent.
     * <P>
     * Type: LONG
     * </P>
     */
    public static final String TIMESTAMP_SENT = "timestamp_sent";

    /**
     * The name of the column containing the time when file is delivered. If 0 means not delivered.
     * <P>
     * Type: LONG
     * </P>
     */
    public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";

    /**
     * The name of the column containing the time when file is displayed. If 0 means not displayed.
     * <P>
     * Type: LONG
     * </P>
     */
    public static final String TIMESTAMP_DISPLAYED = "timestamp_displayed";

    /**
     * The name of the column containing the state of the transfer.
     * <P>
     * Type: INTEGER
     * </P>
     * 
     * @see FileTransfer.State
     */
    public static final String STATE = "state";

    /**
     * The name of the column containing the reason code of the state.
     * <P>
     * Type: INTEGER
     * </P>
     * com.gsma.services.rcs.RcsCommon.ReasonCode
     */
    public static final String REASON_CODE = "reason_code";

    /**
     * The name of the column containing the read status for the transfer.
     * <P>
     * Type: INTEGER
     * </P>
     * FileTransfer.ReadStatus
     */
    public static final String READ_STATUS = "read_status";

    /**
     * The name of the column containing the file icon (absolute path).
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String FILEICON = "fileicon";

    /**
     * The name of the column containing the chat ID used for the file transfer in group chat.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String CHAT_ID = "chat_id";

    /**
     * The name of the column containing the MIME-type of the file icon.
     * <P>
     * Type: TEXT
     * </P>
     */
    public static final String FILEICON_MIME_TYPE = "fileicon_mime_type";
}
