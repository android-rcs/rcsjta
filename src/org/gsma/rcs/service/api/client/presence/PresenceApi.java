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

package org.gsma.rcs.service.api.client.presence;


public class PresenceApi
  extends org.gsma.rcs.service.api.client.ClientApi{
  // Fields

  // Constructors

  public PresenceApi(android.content.Context arg1){
    super((android.content.Context) null);
  }
  // Methods

  public void connectApi(){
  }
  public void disconnectApi(){
  }
  public PresenceInfo getMyPresenceInfo(){
    return (PresenceInfo) null;
  }
  public boolean setMyPresenceInfo(PresenceInfo arg1) throws org.gsma.rcs.service.api.client.ClientApiException{
    return false;
  }
  public boolean inviteContact(java.lang.String arg1) throws org.gsma.rcs.service.api.client.ClientApiException{
    return false;
  }
  public boolean acceptSharingInvitation(java.lang.String arg1) throws org.gsma.rcs.service.api.client.ClientApiException{
    return false;
  }
  public boolean rejectSharingInvitation(java.lang.String arg1) throws org.gsma.rcs.service.api.client.ClientApiException{
    return false;
  }
  public void ignoreSharingInvitation(java.lang.String arg1) throws org.gsma.rcs.service.api.client.ClientApiException{
  }
  public boolean revokeContact(java.lang.String arg1) throws org.gsma.rcs.service.api.client.ClientApiException{
    return false;
  }
  public boolean unrevokeContact(java.lang.String arg1) throws org.gsma.rcs.service.api.client.ClientApiException{
    return false;
  }
  public boolean unblockContact(java.lang.String arg1) throws org.gsma.rcs.service.api.client.ClientApiException{
    return false;
  }
  public java.util.List<java.lang.String> getGrantedContacts() throws org.gsma.rcs.service.api.client.ClientApiException{
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getRevokedContacts() throws org.gsma.rcs.service.api.client.ClientApiException{
    return (java.util.List<java.lang.String>) null;
  }
  public java.util.List<java.lang.String> getBlockedContacts() throws org.gsma.rcs.service.api.client.ClientApiException{
    return (java.util.List<java.lang.String>) null;
  }
}
