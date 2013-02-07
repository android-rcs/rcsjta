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

package org.gsma.rcs.sip;


public class SipApi
  extends org.gsma.rcs.ClientApi{
  // Fields

  // Constructors

  public SipApi(android.content.Context arg1){
    super((android.content.Context) null);
  }
  // Methods

  public void connectApi(){
  }
  public void disconnectApi(){
  }
  public ISipSession getSession(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (ISipSession) null;
  }
  public ISipSession initiateSession(java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws org.gsma.rcs.ClientApiException{
    return (ISipSession) null;
  }
  public java.util.List<android.os.IBinder> getSessionsWith(java.lang.String arg1) throws org.gsma.rcs.ClientApiException{
    return (java.util.List<android.os.IBinder>) null;
  }
  public java.util.List<android.os.IBinder> getSessions() throws org.gsma.rcs.ClientApiException{
    return (java.util.List<android.os.IBinder>) null;
  }
  public boolean sendSipInstantMessage(java.lang.String arg1, java.lang.String arg2, java.lang.String arg3, java.lang.String arg4) throws org.gsma.rcs.ClientApiException{
    return false;
  }
}
