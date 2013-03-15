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

package org.gsma.joyn.messaging;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.String;

/**
 * Interface IMessageDeliveryListener.
* <p>
 * File generated from AIDL
 */
public interface IMessageDeliveryListener extends IInterface {

    /**
     *
     * @param contact
     * @param mesgId
     * @param status
     */
    public void handleMessageDeliveryStatus(String contact, String msgId, String status) throws RemoteException;

    public abstract static class Stub extends Binder implements IMessageDeliveryListener {

        public Stub() {
            super();
        }

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
         * @return  The i message delivery listener.
         */
        public static IMessageDeliveryListener asInterface(IBinder binder) {
            return (IMessageDeliveryListener) null;
        }

    }

}
