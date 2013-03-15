/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.richcall;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.String;

/**
 * Interface IVideoSharingSession.
 *
 * Generated from AIDL.
 */
public interface IVideoSharingSession extends IInterface {
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

    public void acceptSession() throws RemoteException;

    public void rejectSession() throws RemoteException;

    public void cancelSession() throws RemoteException;

    /**
     * Adds a session listener.
     *
     * @param arg1 The arg1.
     */
    public void addSessionListener(IVideoSharingEventListener arg1) throws RemoteException;

    /**
     * Removes a session listener.
     *
     * @param arg1 The arg1.
     */
    public void removeSessionListener(IVideoSharingEventListener arg1) throws RemoteException;

    /**
     * Sets the media renderer.
     *
     * @param arg1 The media renderer.
     */
    public void setMediaRenderer(org.gsma.joyn.media.IMediaRenderer arg1) throws RemoteException;

    /**
     * Class Stub.
     */
    public abstract static class Stub extends Binder implements IVideoSharingSession {
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
        public IBinder asBinder() {
            return (IBinder) null;
        }

        /**
         *
         * @param code
         * @param data
         * @param reply
         * @param flags
         * @return  The boolean.
         */
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            return false;
        }

        /**
         *
         * @param binder
         * @return  The i video sharing session.
         */
        public static IVideoSharingSession asInterface(IBinder binder) {
            return (IVideoSharingSession) null;
        }

    } // end Stub

} // end IVideoSharingSession
