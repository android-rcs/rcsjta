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

import android.os.IBinder;
import java.lang.String;
import java.util.List;

public class MessagingApi extends org.gsma.joyn.ClientApi {

  public MessagingApi(android.content.Context arg1){
    super((android.content.Context) null);
  }

  public void connectApi(){
  }
  public void disconnectApi(){
  }
  public IFileTransferSession transferFile(String arg1, String arg2) throws org.gsma.joyn.ClientApiException{
    return (IFileTransferSession) null;
  }
  public IFileTransferSession transferFileToGroup(List<String> arg1, String arg2) throws org.gsma.joyn.ClientApiException{
    return (IFileTransferSession) null;
  }
  public IFileTransferSession getFileTransferSession(String arg1) throws org.gsma.joyn.ClientApiException{
    return (IFileTransferSession) null;
  }
  public List<IBinder> getFileTransferSessionsWith(String arg1) throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public List<IBinder> getFileTransferSessions() throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public IChatSession initiateOne2OneChatSession(String arg1, String arg2) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession initiateAdhocGroupChatSession(List<String> arg1) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession initiateAdhocGroupChatSession(List<String> arg1, String arg2) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession rejoinGroupChatSession(String arg1) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession restartGroupChatSession(String arg1) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession getChatSession(String arg1) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public List<IBinder> getChatSessionsWith(String arg1) throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public List<IBinder> getChatSessions() throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public List<IBinder> getGroupChatSessions() throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public List<IBinder> getGroupChatSessionsWith(String arg1) throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public void setMessageDeliveryStatus(String arg1, String arg2, String arg3) throws org.gsma.joyn.ClientApiException{
  }
  public void addMessageDeliveryListener(IMessageDeliveryListener arg1) throws org.gsma.joyn.ClientApiException{
  }
  public void removeMessageDeliveryListener(IMessageDeliveryListener arg1) throws org.gsma.joyn.ClientApiException{
  }
}
