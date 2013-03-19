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

/**
 * Class MessagingApi.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class MessagingApi extends org.gsma.joyn.ClientApi {

  public MessagingApi(android.content.Context context){
    super((android.content.Context) null);
  }

  public void connectApi(){
  }
  public void disconnectApi(){
  }
    public IFileTransferSession transferFile(String contact, String file) throws org.gsma.joyn.ClientApiException{
    return (IFileTransferSession) null;
  }
  public IFileTransferSession transferFileToGroup(List<String> arg1, String arg2) throws org.gsma.joyn.ClientApiException{
    return (IFileTransferSession) null;
  }
  public IFileTransferSession getFileTransferSession(String id) throws org.gsma.joyn.ClientApiException{
    return (IFileTransferSession) null;
  }
  public List<IBinder> getFileTransferSessionsWith(String contact) throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public List<IBinder> getFileTransferSessions() throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public IChatSession initiateOne2OneChatSession(String contact, String firstMessage) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession initiateAdhocGroupChatSession(List<String> participants) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession initiateAdhocGroupChatSession(List<String> participants, String subject) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession rejoinGroupChatSession(String chatId) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession restartGroupChatSession(String chatId) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public IChatSession getChatSession(String chatId) throws org.gsma.joyn.ClientApiException{
    return (IChatSession) null;
  }
  public List<IBinder> getChatSessionsWith(String contact) throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public List<IBinder> getChatSessions() throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public List<IBinder> getGroupChatSessions() throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public List<IBinder> getGroupChatSessionsWith(String chatId) throws org.gsma.joyn.ClientApiException{
    return (List<IBinder>) null;
  }
  public void setMessageDeliveryStatus(String contact, String messageId, String status) throws org.gsma.joyn.ClientApiException{
  }
  public void addMessageDeliveryListener(IMessageDeliveryListener listener) throws org.gsma.joyn.ClientApiException{
  }
  public void removeMessageDeliveryListener(IMessageDeliveryListener listener) throws org.gsma.joyn.ClientApiException{
  }
}
