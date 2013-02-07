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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.richcall;

/**
 * Class IRichCallApi.
 */
public interface IRichCallApi extends android.os.IInterface {
    /**
     * Returns the remote phone number.
     *  
     * @return  The remote phone number.
     */
    public String getRemotePhoneNumber() throws android.os.RemoteException;

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The i video sharing session.
     */
    public IVideoSharingSession initiateLiveVideoSharing(String arg1, org.gsma.rcs.media.IMediaPlayer arg2) throws android.os.RemoteException;

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @return  The i video sharing session.
     */
    public IVideoSharingSession initiateVideoSharing(String arg1, String arg2, org.gsma.rcs.media.IMediaPlayer arg3) throws android.os.RemoteException;

    /**
     * Returns the video sharing session.
     *  
     * @param arg1 The arg1.
     * @return  The video sharing session.
     */
    public IVideoSharingSession getVideoSharingSession(String arg1) throws android.os.RemoteException;

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The i image sharing session.
     */
    public IImageSharingSession initiateImageSharing(String arg1, String arg2) throws android.os.RemoteException;

    /**
     * Returns the image sharing session.
     *  
     * @param arg1 The arg1.
     * @return  The image sharing session.
     */
    public IImageSharingSession getImageSharingSession(String arg1) throws android.os.RemoteException;

    /**
     * Sets the multi party call.
     *  
     * @param arg1 The multi party call.
     */
    public void setMultiPartyCall(boolean arg1) throws android.os.RemoteException;

    /**
     * Sets the call hold.
     *  
     * @param arg1 The call hold.
     */
    public void setCallHold(boolean arg1) throws android.os.RemoteException;

    /**
     * Class Stub.
     */
    public abstract static class Stub extends android.os.Binder implements IRichCallApi {
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
         * @return  The i rich call api.
         */
        public static IRichCallApi asInterface(android.os.IBinder arg1) {
            return (IRichCallApi) null;
        }

    } // end Stub

} // end IRichCallApi
