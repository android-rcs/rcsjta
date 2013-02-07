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

/**
 * Class ISipSession.
 */
public interface ISipSession extends android.os.IInterface {
    /**
     * Returns the session i d.
     *  
     * @return  The session i d.
     */
    public String getSessionID() throws android.os.RemoteException;

    /**
     * Returns the remote contact.
     *  
     * @return  The remote contact.
     */
    public String getRemoteContact() throws android.os.RemoteException;

    /**
     * Returns the session state.
     *  
     * @return  The session state.
     */
    public int getSessionState() throws android.os.RemoteException;

    /**
     *  
     * @param arg1 The arg1.
     */
    public void acceptSession(String arg1) throws android.os.RemoteException;

    public void rejectSession() throws android.os.RemoteException;

    public void cancelSession() throws android.os.RemoteException;

    /**
     * Adds a session listener.
     *  
     * @param arg1 The arg1.
     */
    public void addSessionListener(ISipSessionEventListener arg1) throws android.os.RemoteException;

    /**
     * Removes a session listener.
     *  
     * @param arg1 The arg1.
     */
    public void removeSessionListener(ISipSessionEventListener arg1) throws android.os.RemoteException;

    /**
     * Returns the feature tag.
     *  
     * @return  The feature tag.
     */
    public String getFeatureTag() throws android.os.RemoteException;

    /**
     * Returns the local sdp.
     *  
     * @return  The local sdp.
     */
    public String getLocalSdp() throws android.os.RemoteException;

    /**
     * Returns the remote sdp.
     *  
     * @return  The remote sdp.
     */
    public String getRemoteSdp() throws android.os.RemoteException;

    /**
     * Class Stub.
     */
    public abstract static class Stub extends android.os.Binder implements ISipSession {
        /**
         * Creates a new instance of Stub.
         */
        public Stub() {
            super();
        }

        /**
         *  
         * @return  The i binder.
         */
        public android.os.IBinder asBinder() {
            return (android.os.IBinder) null;
        }

        /**
         *  
         * @param arg1 The arg1.
         * @param arg2 The arg2.
         * @param arg3 The arg3.
         * @param arg4 The arg4.
         * @return  The boolean.
         */
        public boolean onTransact(int arg1, android.os.Parcel arg2, android.os.Parcel arg3, int arg4) throws android.os.RemoteException {
            return false;
        }

        /**
         *  
         * @param arg1 The arg1.
         * @return  The i sip session.
         */
        public static ISipSession asInterface(android.os.IBinder arg1) {
            return (ISipSession) null;
        }

    } // end Stub

}
