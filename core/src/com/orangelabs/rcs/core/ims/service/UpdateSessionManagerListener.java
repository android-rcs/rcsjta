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

package com.orangelabs.rcs.core.ims.service;

import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;

/**
 * Listener of events sent to Update Session Manager
 * 
 * @author O. Magnon
 */
public interface UpdateSessionManagerListener {

    /**
     * ReInvite Response received
     * 
     * @param code Sip response code
     * @param response Sip response request
     */
    public void handleReInviteResponse(int code, SipResponse response);

    /**
     * User answer received
     * 
     * @param code user response code
     */
    public void handleReInviteUserAnswer(int code);

    /**
     * ReInvite Ack received
     * 
     * @param code Sip response code
     */
    public void handleReInviteAck(int code);

    /**
     * Sdp requested for ReInvite Response
     * 
     * @param reInvite Sip reInvite request received
     */
    public String buildReInviteSdpResponse(SipRequest reInvite);
}
