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

package org.gsma.rcs.presence;


public interface IPresenceApi
  extends android.os.IInterface
{
  // Classes

  public abstract static class Stub
    extends android.os.Binder    implements IPresenceApi
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
    public static IPresenceApi asInterface(android.os.IBinder arg1){
      return (IPresenceApi) null;
    }
    public boolean onTransact(int arg1, android.os.Parcel arg2, android.os.Parcel arg3, int arg4) throws android.os.RemoteException{
      return false;
    }
  }
  // Methods

  public boolean setMyPresenceInfo(PresenceInfo arg1) throws android.os.RemoteException;
  public boolean inviteContact(java.lang.String arg1) throws android.os.RemoteException;
  public boolean acceptSharingInvitation(java.lang.String arg1) throws android.os.RemoteException;
  public boolean rejectSharingInvitation(java.lang.String arg1) throws android.os.RemoteException;
  public void ignoreSharingInvitation(java.lang.String arg1) throws android.os.RemoteException;
  public boolean revokeContact(java.lang.String arg1) throws android.os.RemoteException;
  public boolean unrevokeContact(java.lang.String arg1) throws android.os.RemoteException;
  public boolean unblockContact(java.lang.String arg1) throws android.os.RemoteException;
  public java.util.List<java.lang.String> getGrantedContacts() throws android.os.RemoteException;
  public java.util.List<java.lang.String> getRevokedContacts() throws android.os.RemoteException;
  public java.util.List<java.lang.String> getBlockedContacts() throws android.os.RemoteException;
}
