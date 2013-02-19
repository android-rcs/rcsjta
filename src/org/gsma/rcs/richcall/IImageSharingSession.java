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

package org.gsma.rcs.richcall;

/**
 * Class IImageSharingSession.
 */
public interface IImageSharingSession extends android.os.IInterface {
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
     * Returns the filename.
     *
     * @return  The filename.
     */
    public String getFilename() throws android.os.RemoteException;

    /**
     * Returns the filesize.
     *
     * @return  The filesize.
     */
    public long getFilesize() throws android.os.RemoteException;

    /**
     * Returns the file thumbnail.
     *
     * @return  The file thumbnail.
     */
    public String getFileThumbnail() throws android.os.RemoteException;

    public void acceptSession() throws android.os.RemoteException;

    public void rejectSession() throws android.os.RemoteException;

    public void cancelSession() throws android.os.RemoteException;

    /**
     * Adds a session listener.
     *
     * @param arg1 The arg1.
     */
    public void addSessionListener(IImageSharingEventListener arg1) throws android.os.RemoteException;

    /**
     * Removes a session listener.
     *
     * @param arg1 The arg1.
     */
    public void removeSessionListener(IImageSharingEventListener arg1) throws android.os.RemoteException;

    /**
     * Class Stub.
     */
    public abstract static class Stub extends android.os.Binder implements IImageSharingSession {
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
         * @param code
         * @param data
         * @param reply
         * @param flags
         * @return  The boolean.
         */
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            return false;
        }

        /**
         *
         * @param arg1 The arg1.
         * @return  The i image sharing session.
         */
        public static IImageSharingSession asInterface(android.os.IBinder arg1) {
            return (IImageSharingSession) null;
        }

    } // end Stub

} // end IImageSharingSession
