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


public enum H264TypeLevel
{
  // Enum Constants

  LEVEL_AUTODETECT((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_1((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_1B((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_1_1((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_1_2((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_1_3((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_2((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_2_1((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_2_2((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_3((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_3_1((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_3_2((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_4((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_4_1((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_4_2((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_5((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
, LEVEL_5_1((java.lang.String) null, 0, 0, (H264TypeLevel.H264ConstraintSetFlagType) null)
;
  // Classes

  public static enum H264ConstraintSetFlagType
  {
    // Enum Constants

    ANY((java.lang.String) null, 0, 0)
, FALSE((java.lang.String) null, 0, 0)
, TRUE((java.lang.String) null, 0, 0)
;
    // Fields

    // Constructors

    private H264ConstraintSetFlagType(java.lang.String arg1, int arg2, int arg3){
    }
    // Methods

    public int getDecimalValue(){
      return 0;
    }
  }
  // Fields

  // Constructors

  private H264TypeLevel(java.lang.String arg1, int arg2, int arg3, H264TypeLevel.H264ConstraintSetFlagType arg4){
  }
  // Methods

  public int getDecimalValue(){
    return 0;
  }
  public static H264TypeLevel getH264LevelType(int arg1, H264TypeLevel.H264ConstraintSetFlagType arg2){
    return (H264TypeLevel) null;
  }
  public H264TypeLevel.H264ConstraintSetFlagType getH264ConstraintSet3Flag(){
    return (H264TypeLevel.H264ConstraintSetFlagType) null;
  }
}
