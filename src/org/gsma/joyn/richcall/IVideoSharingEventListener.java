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

/**
 * Interface IVideoSharingEventListener.
 * <p>
 * Generated from AIDL.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public interface IVideoSharingEventListener extends IInterface {
    public void handleSessionStarted() throws RemoteException;

    /**
     *
     * @param arg1 The arg1.
     */
    public void handleSessionAborted(int arg1) throws RemoteException;

    public void handleSessionTerminatedByRemote() throws RemoteException;

    /**
     *
     * @param arg1 The arg1.
     */
    public void handleSharingError(int arg1) throws RemoteException;

    /**
     * Class Stub.
     */
    public abstract static class Stub extends Binder implements IVideoSharingEventListener {
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
         * @return  The i video sharing event listener.
         */
        public static IVideoSharingEventListener asInterface(IBinder binder) {
            return (IVideoSharingEventListener) null;
        }

    }

}
