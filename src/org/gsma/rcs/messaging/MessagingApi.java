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

package org.gsma.rcs.messaging;


public class MessagingApi
  extends org.gsma.rcs.ClientApi{
  // Fields

  // Constructors

  public MessagingApi(android.content.Context arg1){
    super((android.content.Context) null);
  }
  // Methods

  public void connectApi(){
  }
  public void disconnectApi(){
  }
  public IFileTransferSession transferFile(java.lang.String arg1, java.lang.String arg2) throws org.gsma.rcs.ClientApiException{
    return (IFileTransferSession) null;
  }
  public IFileTransferSession transferFileToGroup(java.util.List<java.lang.String> arg1, java.lang.String arg2) throws org.gsma.rcs.ClientApiException{
    return (IFileTransferSession) null;
  }
  public IFileTransferSession getFileTransferSession(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (IFileTransferSession) null;
  }
  public java.util.List<android.os.IBinder> getFileTransferSessionsWith(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (java.util.List<android.os.IBinder>) null;
  }
  public java.util.List<android.os.IBinder> getFileTransferSessions() throws org.gsma.rcs.ClientApiException{
    return (java.util.List<android.os.IBinder>) null;
  }
  public IChatSession initiateOne2OneChatSession(java.lang.String arg1, java.lang.String arg2) throws org.gsma.rcs.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession initiateAdhocGroupChatSession(java.util.List<java.lang.String> arg1) throws org.gsma.rcs.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession initiateAdhocGroupChatSession(java.util.List<java.lang.String> arg1, java.lang.String arg2) throws org.gsma.rcs.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession rejoinGroupChatSession(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession restartGroupChatSession(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession getChatSession(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (IChatSession) null;
  }
  public java.util.List<android.os.IBinder> getChatSessionsWith(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (java.util.List<android.os.IBinder>) null;
  }
  public java.util.List<android.os.IBinder> getChatSessions() throws org.gsma.rcs.ClientApiException{
    return (java.util.List<android.os.IBinder>) null;
  }
  public java.util.List<android.os.IBinder> getGroupChatSessions() throws org.gsma.rcs.ClientApiException{
    return (java.util.List<android.os.IBinder>) null;
  }
  public java.util.List<android.os.IBinder> getGroupChatSessionsWith(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (java.util.List<android.os.IBinder>) null;
  }
  public void setMessageDeliveryStatus(java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws org.gsma.rcs.ClientApiException{
  }
  public void addMessageDeliveryListener(IMessageDeliveryListener arg1) throws org.gsma.rcs.ClientApiException{
  }
  public void removeMessageDeliveryListener(IMessageDeliveryListener arg1) throws org.gsma.rcs.ClientApiException{
  }
}
