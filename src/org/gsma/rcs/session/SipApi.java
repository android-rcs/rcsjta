/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.gsma.rcs.session;

import android.content.Context;
import android.os.IBinder;
import java.lang.String;
import java.util.List;
import org.gsma.rcs.ClientApiException;

/**
 * SIP session class
 */
public class SipApi extends org.gsma.rcs.ClientApi {

    public SipApi(Context arg1){
        super((Context) null);
    }

    /**
     * Bind to the service service AIDL
     */
    public void connectApi() {
    }

    /**
     * Unbind from the stack service AIDL
     */
    public void disconnectApi() {
    }

    /**
     * Get current SIP session from its session identifier
     * 
     * @param id Session ID
     * @return SIP session object
     * @throws ClientApiException
     */
    public ISipSession getSession(String id) throws ClientApiException {
        return (ISipSession) null;
    }

    /**
     * Get list of current SIP sessions with a contact
     * 
     * @param contact Contact
     * @return SIP session object
     * @throws ClientApiException
     *
     */
    public List<IBinder> getSessionsWith(String contact) throws ClientApiException {
        return (List<IBinder>) null;
    }

    /**
     * Get list of current SIP sessions
     * 
     * @return List of SIP sessions
     * @throws ClientApiException
     */
    public List<IBinder> getSessions() throws ClientApiException {
        return (List<IBinder>) null;
    }

    /**
  	 * Initiate a new SIP session
     *
     * @param contact Contact
     * @param featureTag Feature tag of the service
     * @param sdp SDP
     * @return SIP session object
     * @throws ClientApiException
     */
    public ISipSession initiateSession(String contact, String featureTag, String sdp)
        throws ClientApiException {
        return (ISipSession) null;
    }

    /**
  	 * Send an instant message (SIP MESSAGE)
     * 
     * @param contact Contact
     * @param featureTag Feature tag of the service
     * @param content Content
     * @param contentType Content type
     * @return true if successful, false otherwise
     * @throws ClientApiException
     */
    public boolean sendSipInstantMessage(String contact, String featureTag, String content, String contentType)
        throws ClientApiException {
        return false;
    }

}
