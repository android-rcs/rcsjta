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

package org.gsma.joyn.messaging;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.String;
import java.util.List;

/**
 * Interface IFileTransferSession
 * <p>
 * Generated from AIDL
 */
public interface IFileTransferSession extends IInterface {

  public abstract static class Stub extends Binder implements IFileTransferSession {

    public Stub(){
      super();
    }

    public IBinder asBinder(){
      return (IBinder) null;
    }
    public static IFileTransferSession asInterface(IBinder binder){
      return (IFileTransferSession) null;
    }
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException{
      return false;
    }
  }

  public String getSessionID() throws RemoteException;
  public String getRemoteContact() throws RemoteException;
  public int getSessionState() throws RemoteException;
  public String getFilename() throws RemoteException;
  public long getFilesize() throws RemoteException;
  public String getFileThumbnail() throws RemoteException;
  public void acceptSession() throws RemoteException;
  public void rejectSession() throws RemoteException;
  public void cancelSession() throws RemoteException;
  public void addSessionListener(IFileTransferEventListener arg1) throws RemoteException;
  public void removeSessionListener(IFileTransferEventListener arg1) throws RemoteException;
  public boolean isGroupTransfer() throws RemoteException;
  public boolean isHttpTransfer() throws RemoteException;
  public List<String> getContacts() throws RemoteException;
  public void resumeSession() throws RemoteException;
}
