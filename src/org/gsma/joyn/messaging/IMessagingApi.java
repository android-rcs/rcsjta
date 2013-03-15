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
 * Interface IMessagingApi
 * <p>
 * Generated from AIDL
 */
public interface IMessagingApi extends IInterface {

  public abstract static class Stub extends Binder implements IMessagingApi {

    public Stub(){
      super();
    }

    public IBinder asBinder(){
      return (IBinder) null;
    }

    public static IMessagingApi asInterface(IBinder binder){
      return (IMessagingApi) null;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException{
      return false;
    }
  }

  public IFileTransferSession transferFile(String arg1, String arg2) throws RemoteException;

  public IFileTransferSession transferFileToGroup(List<String> arg1, String arg2) throws RemoteException;

  public IFileTransferSession getFileTransferSession(String arg1) throws RemoteException;

  public List<IBinder> getFileTransferSessionsWith(String arg1) throws RemoteException;

  public List<IBinder> getFileTransferSessions() throws RemoteException;

  public IChatSession initiateOne2OneChatSession(String arg1, String arg2) throws RemoteException;

  public IChatSession initiateAdhocGroupChatSession(List<String> arg1, String arg2) throws RemoteException;

  public IChatSession rejoinGroupChatSession(String arg1) throws RemoteException;

  public IChatSession restartGroupChatSession(String arg1) throws RemoteException;

  public IChatSession getChatSession(String arg1) throws RemoteException;

  public List<IBinder> getChatSessionsWith(String arg1) throws RemoteException;

  public List<IBinder> getChatSessions() throws RemoteException;

  public List<IBinder> getGroupChatSessions() throws RemoteException;

  public List<IBinder> getGroupChatSessionsWith(String arg1) throws RemoteException;

  public void setMessageDeliveryStatus(String arg1, String arg2, String arg3) throws RemoteException;

  public void addMessageDeliveryListener(IMessageDeliveryListener arg1) throws RemoteException;

  public void removeMessageDeliveryListener(IMessageDeliveryListener arg1) throws RemoteException;

}
