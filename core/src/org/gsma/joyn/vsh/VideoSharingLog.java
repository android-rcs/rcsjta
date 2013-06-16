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
package org.gsma.joyn.vsh;

import android.net.Uri;

/**
 * Content provider for video sharing history
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingLog {
    /**
     * Content provider URI
     */
    public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider/vsh");
	
    /**
     * The name of the column containing the unique ID of the video sharing.
     * <P>Type: TEXT</P>
     */
    public static final String SHARING_ID = "sharing_id";

    /**
     * The name of the column containing the MSISDN of the sender.
     * <P>Type: TEXT</P>
     */
    public static final String CONTACT_NUMBER = "contact_number";

    /**
     * The name of the column containing the direction of the sharing
     * <P>Type: INTEGER</P>
     */
    public static final String DIRECTION = "direction";

    /**
     * The name of the column containing the date of the sharing
     * <P>Type: LONG</P>
     */
    public static final String TIMESTAMP = "timestamp";

    /**
     * The name of the column containing the state of the sharing.
     * <P>Type: INTEGER</P>
     */
    public static final String STATE = "state";    
    
    /**
     * The name of the column containing the duration of the sharing.
     * <P>Type: LONG</P>
     */
    public static final String DURATION = "duration";    

    /**
     * State of the sharing
     */
    public static class State {
        /**
         * Outgoing invitation is pending
         */
        public static final int INVITED = 0;
    
        /**
         * Incoming invitation is pending
         */
        public static final int INITIATED = 1;

        /**
         * Invitation has been accepted and sharing is started
         */
        public static final int STARTED = 2;

        /**
         * Sharing has been aborted
         */
        public static final int ABORTED = 4;

        /**
         * Sharing has failed
         */
        public static final int FAILED = 5;
    }

    /**
     * Direction of the sharing
     */
    public static class Direction {
        /**
         * Incoming sharing
         */
        public static final int INCOMING = 0;
        
        /**
         * Outgoing sharing
         */
        public static final int OUTGOING = 1;
    }    
}
