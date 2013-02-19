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

package org.gsma.rcs.messaging;


public interface IFileTransferSession
  extends android.os.IInterface
{
  // Classes

  public abstract static class Stub
    extends android.os.Binder    implements IFileTransferSession
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
    public static IFileTransferSession asInterface(android.os.IBinder arg1){
      return (IFileTransferSession) null;
    }
    public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException{
      return false;
    }
  }
  // Methods

  public java.lang.String getSessionID() throws android.os.RemoteException;
  public java.lang.String getRemoteContact() throws android.os.RemoteException;
  public int getSessionState() throws android.os.RemoteException;
  public java.lang.String getFilename() throws android.os.RemoteException;
  public long getFilesize() throws android.os.RemoteException;
  public java.lang.String getFileThumbnail() throws android.os.RemoteException;
  public void acceptSession() throws android.os.RemoteException;
  public void rejectSession() throws android.os.RemoteException;
  public void cancelSession() throws android.os.RemoteException;
  public void addSessionListener(IFileTransferEventListener arg1) throws android.os.RemoteException;
  public void removeSessionListener(IFileTransferEventListener arg1) throws android.os.RemoteException;
  public boolean isGroupTransfer() throws android.os.RemoteException;
  public boolean isHttpTransfer() throws android.os.RemoteException;
  public java.util.List<java.lang.String> getContacts() throws android.os.RemoteException;
  public void resumeSession() throws android.os.RemoteException;
}
