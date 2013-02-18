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

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.String;
import java.util.List;

/**
 * SIP session interface
 * <p>
 * File generated from AIDL.
 */
public interface ISipApi extends IInterface {

    public abstract static class Stub extends Binder implements ISipApi {

        public Stub() {
            super();
        }

        public IBinder asBinder() {
            return (IBinder) null;
        }

        /**
         * @param code
         * @param data
         * @param reply
         * @param flags
         * @return true if tansaction complete, false otherwise
         */
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            return false;
        }

        /**
         * Cast an IBinder object into an ISipApi interface, generating a proxy if needed
         *
         * @param binder the IBinder
         * @return  The SIP API object
         */
        public static ISipApi asInterface(IBinder binder) {
            return (ISipApi) null;
        }

    }
   
    /**
     * Get current SIP session from its session identifier
     * 
     * @param id Session ID
     * @return SIP session object
     * @throws ClientApiException
     */
    public ISipSession getSession(String id) throws RemoteException;

    /**
     * Get list of current SIP sessions with a contact
     * 
     * @param contact Contact
     * @return SIP session object
     * @throws ClientApiException
     */
    public List<IBinder> getSessionsWith(String contact) throws RemoteException;

    /**
     * Get list of current SIP sessions
     * 
     * @return List of SIP sessions
     * @throws ClientApiException
     */
    public List<IBinder> getSessions() throws RemoteException;

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
        throws RemoteException;

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
        throws RemoteException;

}
