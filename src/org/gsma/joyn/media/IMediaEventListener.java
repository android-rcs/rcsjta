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

package org.gsma.joyn.media;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.String;

/**
 * Interface IMediaEventListener.
 * <p>
 * Generated from AIDL
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public interface IMediaEventListener extends IInterface {

    public void mediaOpened() throws RemoteException;

    public void mediaClosed() throws RemoteException;

    public void mediaStarted() throws RemoteException;

    public void mediaStopped() throws RemoteException;

    /**
     *
     * @param 
     */
    public void mediaError(String error) throws RemoteException;

    /**
     * Class Stub.
     */
    public abstract static class Stub extends Binder implements IMediaEventListener {
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
         * @return  The i media event listener.
         */
        public static IMediaEventListener asInterface(IBinder binder) {
            return (IMediaEventListener) null;
        }

    }

}
