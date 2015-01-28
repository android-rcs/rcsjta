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

package com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.VideoCodec;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Buffer;

/**
 * Reassembles H264 RTP packets into H264 frames, as per RFC 3984
 *
 * @author Deutsche Telekom AG
 */
public class JavaPacketizer extends VideoCodec {

    /**
     * Packetization mode
     *
     * 0 -> Only Single NAL
     * 1 -> Use FU-A if necessary
     *
     * Warning: packetization-mode 1 normally requires the management of STAP-A.
     * But, this is not yet implemented in the stack. Then, there can be some
     * IOT issues if you use this mode.
     */
    public static final int H264_ENABLED_PACKETIZATION_MODE = 1;

    /**
     * Max frame size to H264
     */
    public static int H264_MAX_PACKET_FRAME_SIZE = 1300; // TODO remove the rtp size...

    /**
     * Max number of packets to H264
     */
    public static final int H264_MAX_RTP_PKTS = 32;

    /**
     * Buffer size for FU Indicator and Header
     */
    public static final int H264_FU_HEADER_SIZE = 2;

    /**
     * AVC NAL sequence parameter
     */
    public static final int AVC_NALTYPE_SPS = 7;

    /**
     * AVC NAL picture parameter
     */
    public static final int AVC_NALTYPE_PPS = 8;

    /**
     * Full frame auxiliary buffer (No Packetization)
     */
    private byte fullFrameData[] = new byte[H264_MAX_PACKET_FRAME_SIZE];

    /**
     * Full frame temporary packets buffer (With Packetization)
     */
    private byte packetsData[][] = new byte[H264_MAX_RTP_PKTS][H264_MAX_PACKET_FRAME_SIZE];

    /**
     * Full frame final chunks buffer (With Packetization)
     */
    private Buffer[] outputs = new Buffer[H264_MAX_RTP_PKTS];

    /**
     * Buffer for FU Indicator and Header
     */
    private byte[] h264FU = new byte[H264_FU_HEADER_SIZE];

    /**
     * Previous sent orientation
     */
    private VideoOrientation previousOrientation = new VideoOrientation(CameraOptions.BACK, Orientation.NONE); 

    /**
     * Because packets can come out of order, it is possible that some packets
     * for a newer frame may arrive while an older frame is still incomplete.
     * However, in the case where we get nothing but incomplete frames, we don't
     * want to keep all of them around forever.
     */
    public JavaPacketizer() {
    }

    public int process(Buffer input, Buffer output) {

        if (input == null || output == null) {
            return BUFFER_PROCESSED_FAILED;
        }

        if (!input.isDiscard()) {
            byte[] bufferData = (byte[]) input.getData();
            int bufferDataLength = input.getLength();
            if (input.getLength() < H264_MAX_PACKET_FRAME_SIZE || H264_ENABLED_PACKETIZATION_MODE == 0) {
                if ((fullFrameData == null) || (fullFrameData.length < bufferDataLength)) {
                    fullFrameData = new byte[bufferDataLength];
                }
                System.arraycopy(bufferData, 0, fullFrameData, 0, bufferDataLength);
                if (fullFrameData.length > 0) {
                    // Copy to buffer
                    output.setFormat(input.getFormat());
                    output.setData(fullFrameData);
                    output.setLength(bufferDataLength);
                    output.setOffset(0);
                    output.setTimeStamp(input.getTimeStamp());
                    output.setFlags(Buffer.FLAG_RTP_MARKER | Buffer.FLAG_RTP_TIME);
                    output.setVideoOrientation(null);
                    if (isToAddOrientationHeader(bufferData, input.getVideoOrientation())) {
                        output.setVideoOrientation(input.getVideoOrientation());
                        previousOrientation = input.getVideoOrientation();
                    }

                }
                return BUFFER_PROCESSED_OK;
            }

            output.setFragments(outputs);
            output.setFragmentsSize(0);

            /*
             * First Header - The FU indicator octet has the following format:
             * +---------------+
             * |0|1|2|3|4|5|6|7|
             * +-+-+-+-+-+-+-+-+
             * |F|NRI|  Type   |
             * +---------------+
             */

            // FU Indicator pos = 0
            h264FU[0] = 0;
            h264FU[0] |= (bufferData[0] & 0xe0);// F | NRI
            h264FU[0] |= H264RtpHeaders.AVC_NALTYPE_FUA;

            /*
             * Second Header - The FU header has the following format:
             * +---------------+
             * |0|1|2|3|4|5|6|7|
             * +-+-+-+-+-+-+-+-+
             * |S|E|R|  Type   |
             * +---------------+
             */

            // FU Header pos = 1
            h264FU[1] = 0;
            h264FU[1] |= 0x80;// for the first pkt, the start bit is on
            // copy the original nal type from the stream
            h264FU[1] |= (bufferData[0] & 0x1f);

            // Split frame into pkts
            // for FU-A, we need to consume the first byte with the NAL header
            int startPosBufferData = 1;
            int available = bufferDataLength - 1;// see comment above
            // define max size (not counting with the fuIndicator and fuHeader)
            int maxSize = H264_MAX_PACKET_FRAME_SIZE - h264FU.length;
            int numberOfRtpPkts = 0;
            while (available > maxSize) {

                // >>>>>>>>>>>> create packet >>>>>>>>>>>>
                // Write h264 payload
                System.arraycopy(h264FU, 0, packetsData[numberOfRtpPkts], 0, h264FU.length);

                // Write frame data
                System.arraycopy(bufferData, startPosBufferData, packetsData[numberOfRtpPkts], h264FU.length, maxSize);

                // Copy to buffer
                Buffer buffer = outputs[numberOfRtpPkts];
                if (buffer == null) {
                    buffer = new Buffer();
                }
                buffer.setFormat(input.getFormat());
                buffer.setData(packetsData[numberOfRtpPkts]);
                buffer.setLength(H264_MAX_PACKET_FRAME_SIZE); // Max packet frame size
                buffer.setOffset(0);
                buffer.setTimeStamp(input.getTimeStamp());
                buffer.setFlags(Buffer.FLAG_RTP_TIME);
                buffer.setVideoOrientation(null);

                // Add data buffer to outputs
                outputs[numberOfRtpPkts] = buffer;

                // Increment number of rtp pkts
                numberOfRtpPkts++;
                // <<<<<<<<<<<< create packet <<<<<<<<<<<<

                // -1 to leave room for the last pkt
                if (numberOfRtpPkts >= H264_MAX_RTP_PKTS - 1) {
                    output.setFragments(null);
                    output.setFragmentsSize(0);
                    output.setData(null);
                    output.setDiscard(true);
                    output.setVideoOrientation(null);
                    return OUTPUT_BUFFER_NOT_FILLED;
                    // this frame is too big and needs to be split into more
                    // pkts than we can buffer
                }

                // reset the start bit
                // FU Header pos = 1
                // we need to switch the start bit off
                h264FU[1] &= 0x3f; // 0x7f

                // update variables
                startPosBufferData += maxSize;
                available -= maxSize;
            }

            // write the last chunk of the FU-A

            // set the end bit
            // FU Header pos = 1
            h264FU[1] |= 0x40;// we need to switch the end bit on

            // >>>>>>>>>>>> create packet >>>>>>>>>>>>
            // write h264 payload
            System.arraycopy(h264FU, 0, packetsData[numberOfRtpPkts], 0, h264FU.length);

            // write frame data
            System.arraycopy(bufferData, startPosBufferData, packetsData[numberOfRtpPkts], h264FU.length, available);

            // copy to buffer
            Buffer buffer = outputs[numberOfRtpPkts];
            if (buffer == null) {
                buffer = new Buffer();
            }
            buffer.setFormat(input.getFormat());
            buffer.setData(packetsData[numberOfRtpPkts]);
            buffer.setLength(h264FU.length + available); // H264FU header length + remaining frame chunk size
            buffer.setOffset(0);
            buffer.setTimeStamp(input.getTimeStamp());
            buffer.setFlags(Buffer.FLAG_RTP_MARKER | Buffer.FLAG_RTP_TIME);
            buffer.setVideoOrientation(null);
            if (isToAddOrientationHeader(packetsData[numberOfRtpPkts], input.getVideoOrientation())) {
                buffer.setVideoOrientation(input.getVideoOrientation());
                previousOrientation = input.getVideoOrientation();
            }

            // add data buffer to outputs
            outputs[numberOfRtpPkts] = buffer;
            
            // increment number of rtp pkts
            numberOfRtpPkts++;
            
            // Set outputs size
            output.setFragmentsSize(numberOfRtpPkts);
            // <<<<<<<<<<<< create packet <<<<<<<<<<<<

            return BUFFER_PROCESSED_OK;
        } else {
            output.setDiscard(true);
            return OUTPUT_BUFFER_NOT_FILLED;
        }
    }

    /**
     * Verifies if we need to send the orientation header. The orientation
     * header should be sent if it's the end packet of an I-Frame or if its the
     * end packet of B/P Frames and the orientation has changed.
     *
     * @param h264Frame H264 Frame
     * @param frameOrientation Frame orientation
     * @return <code>True</code> if it's to add, <code>false</code> otherwise.
     */
    private boolean isToAddOrientationHeader(byte[] h264Frame, VideoOrientation frameOrientation) {
        H264RtpHeaders h264Header = new H264RtpHeaders(h264Frame);
        if (h264Header.isIDRSlice()) {
            return true;
        }

        if ((frameOrientation != null && previousOrientation != null && h264Header != null 
                && previousOrientation.getOrientation() != frameOrientation.getOrientation())
                && h264Header.isNonIDRSlice()) {
            return true;
        }

        return false;
    }

}
