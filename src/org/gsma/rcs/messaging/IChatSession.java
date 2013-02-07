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


public interface IChatSession
  extends android.os.IInterface
{
  // Classes

  public abstract static class Stub
    extends android.os.Binder    implements IChatSession
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
    public static IChatSession asInterface(android.os.IBinder arg1){
      return (IChatSession) null;
    }
    public boolean onTransact(int arg1, android.os.Parcel arg2, android.os.Parcel arg3, int arg4) throws android.os.RemoteException{
      return false;
    }
  }
  // Methods

  public java.lang.String getSessionID() throws android.os.RemoteException;
  public java.lang.String getRemoteContact() throws android.os.RemoteException;
  public int getSessionState() throws android.os.RemoteException;
  public void acceptSession() throws android.os.RemoteException;
  public void rejectSession() throws android.os.RemoteException;
  public void cancelSession() throws android.os.RemoteException;
  public void addSessionListener(IChatEventListener arg1) throws android.os.RemoteException;
  public void removeSessionListener(IChatEventListener arg1) throws android.os.RemoteException;
  public void setMessageDeliveryStatus(java.lang.String arg1, java.lang.String arg2) throws android.os.RemoteException;
  public java.lang.String getSubject() throws android.os.RemoteException;
  public java.lang.String getChatID() throws android.os.RemoteException;
  public boolean isGroupChat() throws android.os.RemoteException;
  public boolean isStoreAndForward() throws android.os.RemoteException;
  public InstantMessage getFirstMessage() throws android.os.RemoteException;
  public java.util.List<java.lang.String> getParticipants() throws android.os.RemoteException;
  public int getMaxParticipants() throws android.os.RemoteException;
  public int getMaxParticipantsToBeAdded() throws android.os.RemoteException;
  public void addParticipant(java.lang.String arg1) throws android.os.RemoteException;
  public void addParticipants(java.util.List<java.lang.String> arg1) throws android.os.RemoteException;
  public java.lang.String sendMessage(java.lang.String arg1) throws android.os.RemoteException;
  public java.lang.String sendGeolocation(GeolocPush arg1) throws android.os.RemoteException;
  public void setIsComposingStatus(boolean arg1) throws android.os.RemoteException;
}
