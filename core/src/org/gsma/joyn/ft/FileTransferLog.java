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
package org.gsma.joyn.ft;

import android.net.Uri;

/**
 * Content provider for file transfer history
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferLog {
    /**
     * Content provider URI
     */
    public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.ft/ft");
	
    /**
     * The name of the column containing the unique ID for a row.
     * <P>Type: primary key</P>
     */
    public static final String ID = "_id";

    /**
     * The name of the column containing the unique ID of the file transfer.
     * <P>Type: TEXT</P>
     */
    public static final String FT_ID = "ft_id";

    /**
     * The name of the column containing the MSISDN of the sender.
     * <P>Type: TEXT</P>
     */
    public static final String CONTACT_NUMBER = "contact_number";
	
    /**
     * The name of the column containing the filename (absolute path).
     * <P>Type: TEXT</P>
     */
    public static final String FILENAME = "filename";

    /**
     * The name of the column containing the MIME-type of the file.
     * <P>Type: TEXT</P>
     */
    public static final String MIME_TYPE = "mime_type";
    
    /**
     * The name of the column containing the direction of the transfer
     * <P>Type: INTEGER</P>
	 * @see FileTransfer.Direction
     */
    public static final String DIRECTION = "direction";

    /**
     * The name of the column containing the file size to be transfered (in bytes)
     * <P>Type: LONG</P>
     */
    public static final String FILE_SIZE = "size";
    
    /**
     * The name of the column containing the current transfered size (in bytes)
     * <P>Type: LONG</P>
     */
    public static final String TRANSFERED_SIZE = "size_transfered";

    /**
     * The name of the column containing the date of the transfer
     * <P>Type: LONG</P>
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * The name of the column containing the state of the transfer
     * <P>Type: INTEGER</P>
	 * @see FileTransfer.State
     */
    public static final String STATE = "state";    
}
