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

package org.gsma.rcs.core.ims.protocol.rtp;


public class MediaRegistry
{
  // Fields

  // Constructors

  public MediaRegistry(){
  }
  // Methods

  public static org.gsma.rcs.core.ims.protocol.rtp.format.Format generateFormat(java.lang.String arg1){
    return (org.gsma.rcs.core.ims.protocol.rtp.format.Format) null;
  }
  public static org.gsma.rcs.core.ims.protocol.rtp.codec.Codec [] generateEncodingCodecChain(java.lang.String arg1){
    return (org.gsma.rcs.core.ims.protocol.rtp.codec.Codec []) null;
  }
  public static org.gsma.rcs.core.ims.protocol.rtp.codec.Codec [] generateDecodingCodecChain(java.lang.String arg1){
    return (org.gsma.rcs.core.ims.protocol.rtp.codec.Codec []) null;
  }
  public static java.util.Vector<org.gsma.rcs.core.ims.protocol.rtp.format.video.VideoFormat> getSupportedVideoFormats(){
    return (java.util.Vector) null;
  }
  public static java.util.Vector<org.gsma.rcs.core.ims.protocol.rtp.format.audio.AudioFormat> getSupportedAudioFormats(){
    return (java.util.Vector) null;
  }
  public static boolean isCodecSupported(java.lang.String arg1){
    return false;
  }
}
