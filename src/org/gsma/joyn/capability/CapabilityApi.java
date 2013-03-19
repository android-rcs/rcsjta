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

package org.gsma.joyn.capability;

import java.lang.String;

/**
 * Class CapabilityApi.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class CapabilityApi extends org.gsma.joyn.ClientApi {

  public CapabilityApi(android.content.Context context){
    super((android.content.Context) null);
  }

  public void connectApi() {
  }

  public void disconnectApi() {
  }

  public org.gsma.joyn.contacts.ContactInfo getContactInfo(String contact){
    return (org.gsma.joyn.contacts.ContactInfo) null;
  }

  public Capabilities requestCapabilities(String contact) throws org.gsma.joyn.ClientApiException{
    return (Capabilities) null;
  }

  public void refreshAllCapabilities() throws org.gsma.joyn.ClientApiException{
  }

  public Capabilities getMyCapabilities(){
    return (Capabilities) null;
  }

  public Capabilities getContactCapabilities(String contact){
    return (Capabilities) null;
  }

}
