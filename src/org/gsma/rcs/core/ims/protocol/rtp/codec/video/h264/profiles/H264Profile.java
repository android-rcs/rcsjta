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


public abstract class H264Profile
{
  // Fields

  // Constructors

  public H264Profile(java.lang.String arg1, H264TypeLevel arg2, H264TypeProfile arg3, java.lang.String arg4, int arg5, int arg6, float arg7, int arg8, int arg9, java.lang.String arg10){
  }
  // Methods

  public H264TypeProfile getType(){
    return (H264TypeProfile) null;
  }
  public H264TypeLevel getLevel(){
    return (H264TypeLevel) null;
  }
  public static java.lang.Byte getProfileIOPFromLevelId(java.lang.String arg1){
    return (java.lang.Byte) null;
  }
  public java.lang.String getCodeParams(){
    return (java.lang.String) null;
  }
  public int getVideoWidth(){
    return 0;
  }
  public int getVideoHeight(){
    return 0;
  }
  public float getFrameRate(){
    return 0.0f;
  }
  public int getBitRate(){
    return 0;
  }
  public int getPacketSize(){
    return 0;
  }
  public java.lang.String getLevelId(){
    return (java.lang.String) null;
  }
  public java.lang.String getProfileName(){
    return (java.lang.String) null;
  }
  public java.lang.Byte getProfileIOP(){
    return (java.lang.Byte) null;
  }
  public static java.lang.Byte getProfileIDCFromLevelId(java.lang.String arg1){
    return (java.lang.Byte) null;
  }
  public static java.lang.Byte getLevelIDCFromLevelId(java.lang.String arg1){
    return (java.lang.Byte) null;
  }
  public static H264Profile getProfile(java.lang.String arg1){
    return (H264Profile) null;
  }
}
