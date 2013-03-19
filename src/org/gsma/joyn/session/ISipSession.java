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

package org.gsma.joyn.session;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.String;

/**
 * Class ISipSession.
 * <p>
 * File generated from AIDL.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public interface ISipSession extends IInterface {

    public abstract static class Stub extends Binder implements ISipSession {

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
         * @return true if the transaction is complete, false otherwise
         */
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            return false;
        }

        /**
         * Cast an IBinder object into an ISipSession
         * 
         * @param binder
         * @return The SIP session
         */
        public static ISipSession asInterface(IBinder binder) {
            return (ISipSession) null;
        }

    }

    /**
     * Returns the session i d.
     *
     * @return  The session i d.
     */
    public String getSessionID() throws RemoteException;

    /**
     * Returns the remote contact.
     *
     * @return  The remote contact.
     */
    public String getRemoteContact() throws RemoteException;

    /**
     * Returns the session state.
     *
     * @return  The session state.
     */
    public int getSessionState() throws RemoteException;

    /**
     *
     * @param 
     */
    public void acceptSession(String arg1) throws RemoteException;

    public void rejectSession() throws RemoteException;

    public void cancelSession() throws RemoteException;

    /**
     * Adds a session listener.
     *
     * @param 
     */
    public void addSessionListener(ISipSessionEventListener arg1) throws RemoteException;

    /**
     * Removes a session listener.
     *
     * @param 
     */
    public void removeSessionListener(ISipSessionEventListener arg1) throws RemoteException;

    /**
     * Returns the feature tag.
     *
     * @return  The feature tag.
     */
    public String getFeatureTag() throws RemoteException;

    /**
     * Returns the local sdp.
     *
     * @return  The local sdp.
     */
    public String getLocalSdp() throws RemoteException;

    /**
     * Returns the remote sdp.
     *
     * @return  The remote sdp.
     */
    public String getRemoteSdp() throws RemoteException;

}
