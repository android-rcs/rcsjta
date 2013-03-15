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

import java.lang.String;
import java.util.List;

public class PresenceApi extends org.gsma.joyn.ClientApi{

  public PresenceApi(android.content.Context arg1){
    super((android.content.Context) null);
  }

  public void connectApi(){
  }
  public void disconnectApi(){
  }
  public PresenceInfo getMyPresenceInfo(){
    return (PresenceInfo) null;
  }
  public boolean setMyPresenceInfo(PresenceInfo arg1) throws org.gsma.joyn.ClientApiException{
    return false;
  }
  public boolean inviteContact(String arg1) throws org.gsma.joyn.ClientApiException{
    return false;
  }
  public boolean acceptSharingInvitation(String arg1) throws org.gsma.joyn.ClientApiException{
    return false;
  }
  public boolean rejectSharingInvitation(String arg1) throws org.gsma.joyn.ClientApiException{
    return false;
  }
  public void ignoreSharingInvitation(String arg1) throws org.gsma.joyn.ClientApiException{
  }
  public boolean revokeContact(String arg1) throws org.gsma.joyn.ClientApiException{
    return false;
  }
  public boolean unrevokeContact(String arg1) throws org.gsma.joyn.ClientApiException{
    return false;
  }
  public boolean unblockContact(String arg1) throws org.gsma.joyn.ClientApiException{
    return false;
  }
  public List<String> getGrantedContacts() throws org.gsma.joyn.ClientApiException{
    return (List<String>) null;
  }
  public List<String> getRevokedContacts() throws org.gsma.joyn.ClientApiException{
    return (List<String>) null;
  }
  public List<String> getBlockedContacts() throws org.gsma.joyn.ClientApiException{
    return (List<String>) null;
  }
}
