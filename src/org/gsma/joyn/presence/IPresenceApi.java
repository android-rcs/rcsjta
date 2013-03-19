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

package org.gsma.joyn.presence;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.lang.String;
import java.util.List;

/**
 * Interface IpresenceApi.
 * <p>
 * Generated from AIDL.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public interface IPresenceApi extends IInterface {

  public abstract static class Stub extends Binder implements IPresenceApi {

    public Stub(){
      super();
    }

    public IBinder asBinder(){
      return (IBinder) null;
    }
    public static IPresenceApi asInterface(IBinder binder){
      return (IPresenceApi) null;
    }
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException{
      return false;
    }
  }

  public boolean setMyPresenceInfo(PresenceInfo arg1) throws RemoteException;
  public boolean inviteContact(String arg1) throws RemoteException;
  public boolean acceptSharingInvitation(String arg1) throws RemoteException;
  public boolean rejectSharingInvitation(String arg1) throws RemoteException;
  public void ignoreSharingInvitation(String arg1) throws RemoteException;
  public boolean revokeContact(String arg1) throws RemoteException;
  public boolean unrevokeContact(String arg1) throws RemoteException;
  public boolean unblockContact(String arg1) throws RemoteException;
  public List<String> getGrantedContacts() throws RemoteException;
  public List<String> getRevokedContacts() throws RemoteException;
  public List<String> getBlockedContacts() throws RemoteException;
}
