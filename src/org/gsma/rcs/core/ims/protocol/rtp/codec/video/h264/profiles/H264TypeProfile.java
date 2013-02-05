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

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.profiles;


public enum H264TypeProfile
{
  // Enum Constants

  PROFILE_BASELINE((java.lang.String) null, 0, 0)
, PROFILE_MAIN((java.lang.String) null, 0, 0)
, PROFILE_EXTENDED((java.lang.String) null, 0, 0)
, PROFILE_HIGH((java.lang.String) null, 0, 0)
, PROFILE_HIGH10((java.lang.String) null, 0, 0)
, PROFILE_HIGH422((java.lang.String) null, 0, 0)
, PROFILE_HIGH444((java.lang.String) null, 0, 0)
, PROFILE_CAVLC444((java.lang.String) null, 0, 0)
;
  // Fields

  public int decimalValue;

  // Constructors

  private H264TypeProfile(java.lang.String arg1, int arg2, int arg3){
  }
  // Methods

  public int getDecimalValue(){
    return 0;
  }
  public static H264TypeProfile getH264ProfileType(int arg1){
    return (H264TypeProfile) null;
  }
}
