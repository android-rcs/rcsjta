/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.protocol.rtp.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.orangelabs.rcs.core.ims.protocol.rtp.util.Packet;

/**
 * Abstract RTP packet
 * 
 * @author jexa7410
 * @author Deutsche Telekom
 */
public class RtpPacket extends Packet {
	public Packet base;
	public int marker;
	public int payloadType;
	public int seqnum;
	public long timestamp;
	public int ssrc;
	public int payloadoffset;
	public int payloadlength;
    public boolean extension;
    public RtpExtensionHeader extensionHeader;

	public RtpPacket() {
		super();
	}

	public RtpPacket(Packet packet) {
		super(packet);
		
		base = packet;
	}

	public void assemble(int length) throws IOException {
		this.length = length;
		this.offset = 0;

		ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(length);
		DataOutputStream dataoutputstream = new DataOutputStream(bytearrayoutputstream);
        if (extension) {
            dataoutputstream.writeByte(144);
        } else {
            dataoutputstream.writeByte(128);
        }

		int i = payloadType;
		if (marker == 1) {
			i = payloadType | 0x80;
		}
		dataoutputstream.writeByte((byte) i);
		dataoutputstream.writeShort(seqnum);
		dataoutputstream.writeInt((int) timestamp);
		dataoutputstream.writeInt(ssrc);

        if (extension && extensionHeader != null) {
            // Write extension header id
            dataoutputstream.writeShort(RtpExtensionHeader.RTP_EXTENSION_HEADER_ID);
            // Write extension header length
            dataoutputstream.writeShort(extensionHeader.elementsCount());
            // Write extension element. For now we will only support the orientation element
            for (RtpExtensionHeader.ExtensionElement element : extensionHeader) {
                int orientationElement = (((((element.id & 0xff) << 4) | ((element.data.length - 1) & 0xff)) << 8)
                        | (element.data[0] & 0xff)) << 16;
                dataoutputstream.writeInt(orientationElement);
            }
        }
        dataoutputstream.write(base.data, base.offset, base.length);
		data = bytearrayoutputstream.toByteArray();
	}

	public int calcLength() {
		return payloadlength + 12;
	}
}
