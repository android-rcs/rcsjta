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

package org.gsma.rcs.media;


public interface IMediaPlayer
  extends android.os.IInterface
{
  // Classes

  public abstract static class Stub
    extends android.os.Binder    implements IMediaPlayer
  {
    // Classes

    // Fields

    // Constructors

    public Stub(){
      super();
    }
    // Methods

    public android.os.IBinder asBinder(){
      return (android.os.IBinder) null;
    }
    public static IMediaPlayer asInterface(android.os.IBinder arg1){
      return (IMediaPlayer) null;
    }
    public boolean onTransact(int arg1, android.os.Parcel arg2, android.os.Parcel arg3, int arg4) throws android.os.RemoteException{
      return false;
    }
  }
  // Methods

  public void start() throws android.os.RemoteException;
  public void stop() throws android.os.RemoteException;
  public void close() throws android.os.RemoteException;
  public void open(java.lang.String arg1, int arg2) throws android.os.RemoteException;
  public void addListener(IMediaEventListener arg1) throws android.os.RemoteException;
  public void removeAllListeners() throws android.os.RemoteException;
  public void setMediaCodec(MediaCodec arg1) throws android.os.RemoteException;
  public int getLocalRtpPort() throws android.os.RemoteException;
  public MediaCodec [] getSupportedMediaCodecs() throws android.os.RemoteException;
  public MediaCodec getMediaCodec() throws android.os.RemoteException;
}
