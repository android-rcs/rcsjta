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

package org.gsma.rcs.capability;

public class CapabilityApi extends org.gsma.rcs.ClientApi {

  public CapabilityApi(android.content.Context arg1){
    super((android.content.Context) null);
  }

  public void connectApi() {
  }

  public void disconnectApi() {
  }

  public org.gsma.rcs.contacts.ContactInfo getContactInfo(java.lang.String arg1){
    return (org.gsma.rcs.contacts.ContactInfo) null;
  }

  public Capabilities requestCapabilities(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (Capabilities) null;
  }

  public void refreshAllCapabilities() throws org.gsma.rcs.ClientApiException{
  }

  public Capabilities getMyCapabilities(){
    return (Capabilities) null;
  }

  public Capabilities getContactCapabilities(java.lang.String arg1){
    return (Capabilities) null;
  }

}
