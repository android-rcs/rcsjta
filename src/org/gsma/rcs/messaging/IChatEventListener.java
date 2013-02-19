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


public interface IChatEventListener
  extends android.os.IInterface
{
  // Classes

  public abstract static class Stub
    extends android.os.Binder    implements IChatEventListener
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
    public static IChatEventListener asInterface(android.os.IBinder arg1){
      return (IChatEventListener) null;
    }
    public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException{
      return false;
    }
  }
  // Methods

  public void handleSessionStarted() throws android.os.RemoteException;
  public void handleSessionAborted(int arg1) throws android.os.RemoteException;
  public void handleSessionTerminatedByRemote() throws android.os.RemoteException;
  public void handleMessageDeliveryStatus(java.lang.String arg1, java.lang.String arg2) throws android.os.RemoteException;
  public void handleReceiveMessage(InstantMessage arg1) throws android.os.RemoteException;
  public void handleImError(int arg1) throws android.os.RemoteException;
  public void handleIsComposingEvent(java.lang.String arg1, boolean arg2) throws android.os.RemoteException;
  public void handleConferenceEvent(java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws android.os.RemoteException;
  public void handleAddParticipantSuccessful() throws android.os.RemoteException;
  public void handleAddParticipantFailed(java.lang.String arg1) throws android.os.RemoteException;
}
