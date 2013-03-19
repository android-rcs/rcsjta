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
 * Interface IMediaPlayer
 * <p>
 * Generated from AIDL
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public interface IMediaPlayer extends IInterface {

  public abstract static class Stub extends Binder implements IMediaPlayer {

    public Stub(){
      super();
    }

    public IBinder asBinder(){
      return (IBinder) null;
    }
    public static IMediaPlayer asInterface(IBinder binder){
      return (IMediaPlayer) null;
    }
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException{
      return false;
    }
  }

  public void start() throws RemoteException;
  public void stop() throws RemoteException;
  public void close() throws RemoteException;
  public void open(String arg1, int arg2) throws RemoteException;
  public void addListener(IMediaEventListener arg1) throws RemoteException;
  public void removeAllListeners() throws RemoteException;
  public void setMediaCodec(MediaCodec arg1) throws RemoteException;
  public int getLocalRtpPort() throws RemoteException;
  public MediaCodec [] getSupportedMediaCodecs() throws RemoteException;
  public MediaCodec getMediaCodec() throws RemoteException;
}
