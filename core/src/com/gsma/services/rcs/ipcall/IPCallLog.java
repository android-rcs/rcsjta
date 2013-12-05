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
package com.gsma.services.rcs.ipcall;

import android.net.Uri;

/**
 * Content provider for IP call history
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallLog {
    /**
     * Content provider URI
     */
    public static final Uri CONTENT_URI = Uri.parse("content://com.gsma.services.rcs.provider.ipcall/ipcall");

    /**
     * The name of the column containing the unique ID for a row.
     * <P>Type: primary key</P>
     */
    public static final String ID = "_id";
    
    /**
     * The name of the column containing the unique ID of the call.
     * <P>Type: TEXT</P>
     */
    public static final String CALL_ID = "call_id";

    /**
     * The name of the column containing the MSISDN of the remote party.
     * <P>Type: TEXT</P>
     */
    public static final String CONTACT_NUMBER = "contact_number";

    /**
     * The name of the column containing the direction of the call.
     * <P>Type: INTEGER</P>
	 * @see IPCall.Direction
     */
    public static final String DIRECTION = "direction";

    /**
     * The name of the column containing the date of the call (in milliseconds).
     * <P>Type: LONG</P>
     */
    public static final String TIMESTAMP = "timestamp";
    
    /**
     * The name of the column containing the duration of the call (in seconds). The
     * value is only set at the end of the call.
     * <P>Type: LONG</P>
     */
    public static final String DURATION = "duration";    

    /**
     * The name of the column containing the state of the call.
     * <P>Type: INTEGER</P>
	 * @see IPCall.State
     */
    public static final String STATE = "state";    
}
