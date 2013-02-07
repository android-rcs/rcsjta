/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.gsma;


public class GsmaClientConnector
{
  // Fields

  public static final java.lang.String GSMA_PREFS_NAME = "gsma.joyn.preferences";

  public static final java.lang.String GSMA_CLIENT = "gsma.joyn.client";

  public static final java.lang.String GSMA_CLIENT_ENABLED = "gsma.joyn.enabled";

  // Constructors

  public GsmaClientConnector(){
  }
  // Methods

  public static boolean isDeviceRcsCompliant(android.content.Context arg1){
    return false;
  }
  public static java.util.Vector<android.content.pm.ApplicationInfo> getRcsClients(android.content.Context arg1){
    return (java.util.Vector<android.content.pm.ApplicationInfo>) null;
  }
  public static boolean isRcsClientActivated(android.content.Context arg1, java.lang.String arg2){
    return false;
  }
  public static android.content.Intent getRcsSettingsActivityIntent(android.content.Context arg1, java.lang.String arg2){
    return (android.content.Intent) null;
  }
}
