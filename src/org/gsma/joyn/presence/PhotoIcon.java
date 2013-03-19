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

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.String;

/**
 * Class PhotoIcon.
 *
 * @author Jean-Marc AUFFRET (Orange)
 * @version 1.0
 * @since 1.0
 */
public class PhotoIcon implements Parcelable {

  public static final Parcelable.Creator<PhotoIcon> CREATOR = null;

  public PhotoIcon(byte [] content, int width, int height, String etag) {
  }
  public PhotoIcon(byte [] content, int width, int height) {
  }
  public PhotoIcon(Parcel source){
  }
  public String toString(){
    return (String) null;
  }
  public String getType(){
    return (String) null;
  }
  public long getSize(){
    return 0l;
  }
  public byte [] getContent(){
    return (byte []) null;
  }
  public int describeContents(){
    return 0;
  }
  public void writeToParcel(Parcel dest, int flags){
  }
  public int getWidth(){
    return 0;
  }
  public int getHeight(){
    return 0;
  }
  public String getEtag(){
    return (String) null;
  }
  public void setEtag(String etag1){
  }
  public String getResolution(){
    return (String) null;
  }
}
