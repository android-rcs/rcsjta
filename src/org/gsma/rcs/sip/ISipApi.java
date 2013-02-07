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


public interface ISipApi extends android.os.IInterface {
  // Classes

  public abstract static class Stub extends android.os.Binder implements ISipApi {
    // Classes

    // Fields

    // Constructors

    public Stub(){
      super();
    }
    // Methods

    public android.os.IBinder asBinder(){
      return (android.os.IBinder) null;
    }
    public static ISipApi asInterface(android.os.IBinder arg1){
      return (ISipApi) null;
    }
    public boolean onTransact(int arg1, android.os.Parcel arg2, android.os.Parcel arg3, int arg4) throws android.os.RemoteException{
      return false;
    }
  }
  // Methods

  public ISipSession getSession(java.lang.String arg1) throws android.os.RemoteException;
  public ISipSession initiateSession(java.lang.String arg1, java.lang.String arg2, java.lang.String arg3) throws android.os.RemoteException;
  public java.util.List<android.os.IBinder> getSessionsWith(java.lang.String arg1) throws android.os.RemoteException;
  public java.util.List<android.os.IBinder> getSessions() throws android.os.RemoteException;
  public boolean sendSipInstantMessage(java.lang.String arg1, java.lang.String arg2, java.lang.String arg3, java.lang.String arg4) throws android.os.RemoteException;
}
