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
import java.util.List;

/**
 * Interface IChatSession
 * <p>
 * Generated from AIDL
 */
public interface IChatSession extends IInterface {

  public abstract static class Stub extends Binder implements IChatSession {

    public Stub() {
      super();
    }

    public IBinder asBinder(){
      return (IBinder) null;
    }

    public static IChatSession asInterface(IBinder binder) {
      return (IChatSession) null;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
      return false;
    } 
  }

  public String getSessionID() throws RemoteException;
  public String getRemoteContact() throws RemoteException;
  public int getSessionState() throws RemoteException;
  public void acceptSession() throws RemoteException;
  public void rejectSession() throws RemoteException;
  public void cancelSession() throws RemoteException;
  public void addSessionListener(IChatEventListener arg1) throws RemoteException;
  public void removeSessionListener(IChatEventListener arg1) throws RemoteException;
  public void setMessageDeliveryStatus(String arg1, String arg2) throws RemoteException;
  public String getSubject() throws RemoteException;
  public String getChatID() throws RemoteException;
  public boolean isGroupChat() throws RemoteException;
  public boolean isStoreAndForward() throws RemoteException;
  public InstantMessage getFirstMessage() throws RemoteException;
  public java.util.List<String> getParticipants() throws RemoteException;
  public int getMaxParticipants() throws RemoteException;
  public int getMaxParticipantsToBeAdded() throws RemoteException;
  public void addParticipant(String arg1) throws RemoteException;
  public void addParticipants(java.util.List<String> arg1) throws RemoteException;
  public String sendMessage(String arg1) throws RemoteException;
  public String sendGeolocation(GeolocPush arg1) throws RemoteException;
  public void setIsComposingStatus(boolean arg1) throws RemoteException;

}
