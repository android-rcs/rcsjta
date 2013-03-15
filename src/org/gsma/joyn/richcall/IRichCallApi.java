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

package org.gsma.joyn.richcall;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.String;
import org.gsma.joyn.media.IMediaPlayer;

/**
 * Interface IRichCallApi.
 * <p>
 * Generated from AIDL
 */
public interface IRichCallApi extends IInterface {

    /**
     * Returns the remote phone number involved in the current call
     *
     * @return  The remote phone number.
     */
    public String getRemotePhoneNumber() throws RemoteException;

    /**
     *	Initiate a live video sharing session
     *
     * @param contact The contact.
     * @param player The player.
     * @return  The i video sharing session.
     */
    public IVideoSharingSession initiateLiveVideoSharing(String contact, IMediaPlayer player) throws RemoteException;

    /**
     * Initiate a pre-recorded video sharing session
     *
     * @param contact The contact.
     * @param file The file.
     * @param player The player
     * @return  The i video sharing session.
     */
    public IVideoSharingSession initiateVideoSharing(String contact, String file, IMediaPlayer player) throws RemoteException;

    /**
     *	Get the current video sharing session from its session ID
     *
     * @param id The id.
     * @return  The video sharing session.
     */
    public IVideoSharingSession getVideoSharingSession(String id) throws RemoteException;

    /**
     *	Initiate an image sharing session
     *
     * @param contact The contact.
     * @param file The file.
     * @return  The i image sharing session.
     */
    public IImageSharingSession initiateImageSharing(String contact, String file) throws RemoteException;

    /**
     * Returns the image sharing session.
     *
     * @param id The id.
     * @return  The image sharing session.
     */
    public IImageSharingSession getImageSharingSession(String id) throws RemoteException;

    /**
     * Sets the multi party call.
     *
     * @param flag The multi party call.
     */
    public void setMultiPartyCall(boolean flag) throws RemoteException;

    /**
     * Sets the call hold.
     *
     * @param flag The call hold.
     */
    public void setCallHold(boolean flag) throws RemoteException;

    /**
     * Class Stub.
     */
    public abstract static class Stub extends Binder implements IRichCallApi {
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
         * @param arg1 The arg1.
         * @return  The i rich call api.
         */
        public static IRichCallApi asInterface(IBinder binder) {
            return (IRichCallApi) null;
        }

    }

}
