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

package org.gsma.rcs.core.ims.protocol.rtp.core;


public class RtcpPacketTransmitter
  extends java.lang.Thread{
  // Fields

  public org.gsma.rcs.platform.network.DatagramConnection datagramConnection;

  // Constructors

  public RtcpPacketTransmitter(java.lang.String arg1, int arg2, RtcpSession arg3) throws java.io.IOException{
    super();
  }
  public RtcpPacketTransmitter(java.lang.String arg1, int arg2, RtcpSession arg3, org.gsma.rcs.platform.network.DatagramConnection arg4) throws java.io.IOException{
    super();
  }
  // Methods

  public void run(){
  }
  public void close() throws java.io.IOException{
  }
  public java.util.Vector<RtcpSdesPacket> makereports(){
    return (java.util.Vector<RtcpSdesPacket>) null;
  }
  public void sendByePacket(){
  }
  public RtcpStatisticsTransmitter getStatistics(){
    return (RtcpStatisticsTransmitter) null;
  }
}
