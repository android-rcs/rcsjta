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

package org.gsma.rcs.media;

public interface IMediaRenderer extends android.os.IInterface {

    /**
     *
     */
    public void open(java.lang.String remoteHost, int remotePort) throws android.os.RemoteException;

    /**
     *
     */
    public void start() throws android.os.RemoteException;

    /**
     *
     */
    public void stop() throws android.os.RemoteException;
    /**
     *
     */
    public void close() throws android.os.RemoteException;

    /**
     *
     */
    public void addListener(IMediaEventListener listener) throws android.os.RemoteException;

    /**
     *
     */
    public void removeAllListeners() throws android.os.RemoteException;

    /**
     *
     */
    public void setMediaCodec(MediaCodec arg1) throws android.os.RemoteException;

    /**
     *
     */
    public int getLocalRtpPort() throws android.os.RemoteException;

    /**
     *
     */
    public MediaCodec [] getSupportedMediaCodecs() throws android.os.RemoteException;

    /**
     *
     */
    public MediaCodec getMediaCodec() throws android.os.RemoteException;


    public abstract static class Stub extends android.os.Binder implements IMediaRenderer {

        public Stub(){
            super();
        }

        public android.os.IBinder asBinder(){
            return (android.os.IBinder) null;
        }

        public static IMediaRenderer asInterface(android.os.IBinder arg1){
            return (IMediaRenderer) null;
        }

        /**
         * @param code
         * @param data
         * @param reply
         * @param flags
         * @return true if the transaction is complete, false otherwise
         */
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException{
            return false;
        }

    }

}
