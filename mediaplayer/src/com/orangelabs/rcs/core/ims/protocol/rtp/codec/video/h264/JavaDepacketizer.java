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
import com.orangelabs.rcs.core.ims.protocol.rtp.format.Format;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Buffer;

/**
 * Reassembles H264 RTP packets into H264 frames, as per RFC 3984 Complete
 * frames are sent to decoder once reassembled
 *
 * @author Deutsche Telekom AG
 */
public class JavaDepacketizer extends VideoCodec {

    /**
     * Collection of frameAssemblers. Allows the construction of several frames
     * if incoming packets are out of order
     */
    private FrameAssemblerCollection assemblersCollection = new FrameAssemblerCollection();

    /**
     * Max frame size to give for next module, as some decoder have frame size
     * limits
     */
    private static final int MAX_H264_FRAME_SIZE = 8192;

    /**
     * Default frame packet size
     */
    public static int H264_FRAME_PACKET_SIZE = 1500;

    /**
     * Video decoder max payloads chunks mask
     */
    private static final byte VIDEO_DECODER_MAX_PAYLOADS_CHUNKS_MASK = 0x1F;

    /**
     * Packet NalUnitHeader
     */
    private NalUnitHeader mNalUnitHeader;

    /**
     * Reading position for aggregation packet
     */
    private int aggregationPositon = 1;

    /**
     * Constructor
     */
    public JavaDepacketizer() {
    }

    /**
     * Performs the media processing defined by this codec
     *
     * @param input The buffer that contains the media data to be processed
     * @param output The buffer in which to store the processed media data
     * @return Processing result
     */
    public int process(Buffer input, Buffer output) {
        if (input == null || output == null) {
            return BUFFER_PROCESSED_FAILED;
        }

        // Extracts the NAL Unit Header from the Input Buffer
        extractNalUnitHeader(input);

        if (mNalUnitHeader.isFragmentationUnit()) {
            return handleFragmentationUnitPacket(input, output);
        } else if (mNalUnitHeader.isAggregationPacket()) {
            return handleAggregationPacket(input, output);
        } else {
            return handleSingleNalUnitPacket(input, output);
        }

    }

    /**
     * Extract the NAL unit header
     *
     * @param input
     */
    private void extractNalUnitHeader(Buffer input) {
        if (mNalUnitHeader == null) {
            mNalUnitHeader = NalUnitHeader.extract((byte[])input.getData());
        } else {
            NalUnitHeader.extract((byte[])input.getData(), mNalUnitHeader);
        }
    }

    /**
     * Extract the NAL unit header at position
     *
     * @param input
     * @param position
     */
    private void extractNalUnitHeader(int position, Buffer input) {
        if (mNalUnitHeader == null) {
            mNalUnitHeader = NalUnitHeader.extract(position, (byte[])input.getData());
        } else {
            NalUnitHeader.extract(position, (byte[])input.getData(), mNalUnitHeader);
        }
    }

    /**
     * Handle single NAL Unit packet
     *
     * @return Processing result
     */
    private int handleSingleNalUnitPacket(Buffer input, Buffer output) {
        // Create output buffer
        byte[] bufferData = (byte[]) input.getData();
        int bufferDataLength = bufferData.length;
        byte[] data = new byte[bufferDataLength];
        System.arraycopy(bufferData, 0, data, 0, bufferDataLength);

        // Set buffer
        output.setData(data);
        output.setLength(data.length);
        output.setOffset(0);
        output.setTimeStamp(input.getTimeStamp());
        output.setSequenceNumber(input.getSequenceNumber());
        output.setVideoOrientation(input.getVideoOrientation());
        output.setFormat(input.getFormat());
        output.setFlags(input.getFlags());

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Handle Aggregation NAL Unit packet
     *
     * @return Processing result
     */
    private int handleAggregationPacket(Buffer input, Buffer output) {
        // Get data
        byte[] bufferData = (byte[]) input.getData();
        if (aggregationPositon + 1 >= bufferData.length) {
            // No more data in aggregation packet
            aggregationPositon = 1;
            output.setDiscard(true);
            return BUFFER_PROCESSED_OK;
        }

        // Get NALU size
        int nalu_size = (((bufferData[aggregationPositon] & 0xff) << 8) | (bufferData[aggregationPositon + 1] & 0xff));
        aggregationPositon+=2;
        if (aggregationPositon + nalu_size > bufferData.length) {
            // Not a correct packet
            aggregationPositon = 1;
            return BUFFER_PROCESSED_FAILED;
        }

        // Get NALU HDR
        extractNalUnitHeader(aggregationPositon, input);
        if (mNalUnitHeader.isSingleNalUnitPacket()) {
            // Create output buffer
            byte[] data = new byte[nalu_size];
            System.arraycopy(bufferData, aggregationPositon, data, 0, nalu_size);
            aggregationPositon+=nalu_size;

            // Set buffer
            output.setData(data);
            output.setLength(data.length);
            output.setOffset(0);
            output.setTimeStamp(input.getTimeStamp());
            output.setSequenceNumber(input.getSequenceNumber());
            output.setVideoOrientation(input.getVideoOrientation());
            output.setFormat(input.getFormat());
            output.setFlags(input.getFlags());

            return INPUT_BUFFER_NOT_CONSUMED;
        } else {
            // Not a correct packet
            aggregationPositon = 1;
            return BUFFER_PROCESSED_FAILED;
        }
    }

    /**
     * Handle Fragmentation NAL Unit packet
     *
     * @return Processing result
     */
    private int handleFragmentationUnitPacket(Buffer input, Buffer output) {
        if (!input.isDiscard()) {
            assemblersCollection.put(input);
            if (assemblersCollection.getLastActiveAssembler().complete()) {
                assemblersCollection.getLastActiveAssembler().copyToBuffer(output);
                assemblersCollection.removeOldestThan(input.getTimeStamp());
                return BUFFER_PROCESSED_OK;
            } else {
                output.setDiscard(true);
                return OUTPUT_BUFFER_NOT_FILLED;
            }
        } else {
            output.setDiscard(true);
            return OUTPUT_BUFFER_NOT_FILLED;
        }
    }

    /**
     * Used to assemble fragments with the same timestamp into a single frame.
     */
    public static class FrameAssembler {
        private byte[][] reassembledData = null; // Frame sequence chunks
        private int[] reassembledDataSize = null; // Sequence chunk size
        private int reassembledDataFullSize = 0; // Frame sequence chunks full size
        private boolean reassembledDataHasStart = false; // Has start chunk
        private boolean reassembledDataHasEnd = false; // Has end chunk
        private int reassembledDataPosSeqStart = Integer.MAX_VALUE; // Pos seq start
        private int reassembledDataPosSeqEnd = Integer.MIN_VALUE; // Pos seq end
        private byte reassembledDataNALHeader = 0; // Final frame NAL header
        private long timeStamp = -1;
        private Format format = null;
        private long seqNumber = -1;
        private VideoOrientation videoOrientation;

        /**
         * Add the buffer (which contains a fragment) to the assembler.
         *
         * @param buffer
         */
        public void put(Buffer buffer) {

            if (buffer.getLength() <= 2) {
                // no actual data in buffer, no need to keep. Typically
                // happens when RTP marker is set.
                return;
            }

            byte[] currentRtpPacketData = ((byte[]) buffer.getData());
            H264RtpHeaders h264RtpHeaders = new H264RtpHeaders(currentRtpPacketData);

            // Forbidden zero bit, must be zero for a valid stream
            if (h264RtpHeaders.getFUI_F()) {
                return;
            }

            if (reassembledData == null) {
                // First packet
                timeStamp = buffer.getTimeStamp();
                format = buffer.getFormat();
                seqNumber = buffer.getSequenceNumber();

                // Get NAL header
                reassembledDataNALHeader = h264RtpHeaders.getNALHeader();

                // Copy packet data to reassembledData
                reassembledData = new byte[JavaPacketizer.H264_MAX_RTP_PKTS][H264_FRAME_PACKET_SIZE];
                reassembledDataSize = new int[JavaPacketizer.H264_MAX_RTP_PKTS];
                reassembledDataHasStart = false;
                reassembledDataHasEnd = false;
            }

            // Sequence position on frame
            int posSeq = (int) (buffer.getSequenceNumber() & VIDEO_DECODER_MAX_PAYLOADS_CHUNKS_MASK);

            // Exclude header size
            int payloadStartPosition = h264RtpHeaders.getHeaderSize();
            // Exclude header size
            int payloadLength = buffer.getLength() - h264RtpHeaders.getHeaderSize();

            // Fragmentation Units (FU-A) have NALs separated through several
            // RTP packets
            if (h264RtpHeaders.getFUI_TYPE() == H264RtpHeaders.AVC_NALTYPE_FUA) {

                // Fill Has Start Chunk
                reassembledDataHasStart |= (h264RtpHeaders.getFUH_S());
                // Fill Has End Chunk
                reassembledDataHasEnd |= (h264RtpHeaders.getFUH_E());

                // Fill Pos Seq Start
                reassembledDataPosSeqStart = ((h264RtpHeaders.getFUH_S()) ? posSeq
                        : reassembledDataPosSeqStart);
                // Fill Pos Seq End
                reassembledDataPosSeqEnd = ((h264RtpHeaders.getFUH_E()) ? posSeq
                        : reassembledDataPosSeqEnd);
            }

            // Sequence chuck size
            reassembledDataSize[posSeq] = payloadLength;

            // Sum chucks total sizes
            reassembledDataFullSize += payloadLength;

            // Copy data
            System.arraycopy(currentRtpPacketData, payloadStartPosition, reassembledData[posSeq],
                    0, payloadLength);
            videoOrientation = buffer.getVideoOrientation();
        }

        /**
         * Is the frame complete?
         */
        public boolean complete() {

            if (!reassembledDataHasStart || !reassembledDataHasEnd) {
                return false; // has start and end chunk
            }

            // Validate chunk sizes between start and end pos
            int posCurrent = reassembledDataPosSeqStart;
            while ((posCurrent & VIDEO_DECODER_MAX_PAYLOADS_CHUNKS_MASK) != reassembledDataPosSeqEnd) {
                // need more data?
                if (reassembledDataSize[posCurrent & VIDEO_DECODER_MAX_PAYLOADS_CHUNKS_MASK] <= 0) {
                    return false;
                }
                posCurrent++;
            }
            // Validate last chunk
            if (reassembledDataSize[reassembledDataPosSeqEnd] <= 0) {
                return false;
            }

            // TODO: if some of the last ones come in after the marker, there
            // will be blank squares in the lower right.
            return true;
        }

        /**
         * Assumes that complete() has been called and returns true.
         */
        private void copyToBuffer(Buffer bDest) {

            if (reassembledDataFullSize <= MAX_H264_FRAME_SIZE) {
                // + 1 because of the header size
                byte[] finalData = new byte[reassembledDataFullSize + 1];
                int finalDataPos = 0;

                // Copy NAL header
                finalData[finalDataPos] = reassembledDataNALHeader;
                finalDataPos += 1;

                // Copy chunk data between start and end pos
                int posCurrent = reassembledDataPosSeqStart;
                int posSeq = 0;
                while ((posCurrent & VIDEO_DECODER_MAX_PAYLOADS_CHUNKS_MASK) != reassembledDataPosSeqEnd) {
                    // need more data?
                    posSeq = posCurrent & VIDEO_DECODER_MAX_PAYLOADS_CHUNKS_MASK;

                    // Copy data
                    System.arraycopy(reassembledData[posSeq], 0, finalData, finalDataPos,
                            reassembledDataSize[posSeq]);
                    finalDataPos += reassembledDataSize[posSeq];

                    posCurrent++;
                }

                // Copy last chunk data
                System.arraycopy(reassembledData[reassembledDataPosSeqEnd], 0, finalData,
                        finalDataPos, reassembledDataSize[reassembledDataPosSeqEnd]);

                // If the frame data can be processed by native module, ie
                // reassembled frame size not too big
                // Set buffer
                bDest.setData(finalData);
                bDest.setLength(reassembledDataSize[reassembledDataPosSeqEnd]);
                bDest.setOffset(0);
                bDest.setTimeStamp(timeStamp);
                bDest.setFormat(format);
                bDest.setFlags(Buffer.FLAG_RTP_MARKER | Buffer.FLAG_RTP_TIME);
                bDest.setVideoOrientation(videoOrientation);
                bDest.setSequenceNumber(seqNumber);
            }

            // Set reassembledData to null
            reassembledData = null;
        }

        /**
         * Reset the FrameAssembler
         *
         * It as package access instead of private for improved performance.
         * See: http://developer.android.com/guide/practices/performance.html 
         *   Consider Package Instead of Private Access with Private Inner Classes
         */
        private void reset() {
            reassembledData = null;
            reassembledDataSize = null;
            reassembledDataFullSize = 0;
            reassembledDataHasStart = false;
            reassembledDataHasEnd = false;
            reassembledDataPosSeqStart = Integer.MAX_VALUE;
            reassembledDataPosSeqEnd = Integer.MIN_VALUE;
            reassembledDataNALHeader = 0;
            timeStamp = -1;
            format = null;
        }

        /**
         * Get timestamp
         *
         * @return long
         */
        public long getTimeStamp() {
            return timeStamp;
        }
    }

    /**
     * Used to manage different timestamps, as packets could be coming not in
     * order. Data is an array of FrameAssemblers, sorted by timestamps (oldest
     * is first, newest is last)
     */
    public static class FrameAssemblerCollection {
        private final static int NUMBER_OF_ASSEMBLERS = 5;
        private FrameAssembler[] assemblers = new FrameAssembler[NUMBER_OF_ASSEMBLERS];
        private int activeAssembler = 0;
        private int numberOfAssemblers = 0;

        /**
         * Add the buffer (which contains a fragment) to the right assembler.
         *
         * @param buffer
         */
        public void put(Buffer buffer) {
            activeAssembler = getAssembler(buffer.getTimeStamp());
            assemblers[activeAssembler].put(buffer);
        }

        /**
         * Get the active frame assembler
         *
         * @return frameAssembler Last active assembler
         */
        public FrameAssembler getLastActiveAssembler() {
            return assemblers[activeAssembler];
        }

        /**
         * Create a new frame assembler for given timeStamp
         *
         * @param timeStamp
         * @return assembler number Position of the assembler in the collection
         */
        public int createNewAssembler(long timeStamp) {
            int spot = -1;
            if (numberOfAssemblers < NUMBER_OF_ASSEMBLERS) {
                // If there's enough space left to create a new assembler
                // We search its spot
                for (int i = 0; i < numberOfAssemblers; i++) {
                    if (timeStamp < assemblers[i].getTimeStamp()) {
                        spot = i;
                    }
                }
                if (spot == -1) {
                    spot = numberOfAssemblers;
                }
                numberOfAssemblers++;

                // Store the assembler that will be "discarded" and can be reused
                FrameAssembler oldAssembler = assemblers[numberOfAssemblers - 1];

                // Decale all assemblers with newest timeStamp to the right
                for (int i = numberOfAssemblers - 1; i > spot; i--) {
                    assemblers[i] = assemblers[i - 1];
                }
                if (oldAssembler != null) {
                    // Reuse and reset the discarded assembler
                    assemblers[spot] = oldAssembler;
                    assemblers[spot].reset();
                } else {
                    assemblers[spot] = new FrameAssembler();
                }
            } else {
                // Store the assembler that will be "discarded" and can be reused
                FrameAssembler oldAssembler = assemblers[0];

                // Not enough space, we destroy the oldest assembler
                for (int i = 1; i < NUMBER_OF_ASSEMBLERS; i++) {
                    assemblers[i - 1] = assemblers[i];
                }
                // Last spot is for the new assembler
                spot = NUMBER_OF_ASSEMBLERS - 1;
                if (oldAssembler != null) {
                    // Reuse and reset the discarded assembler
                    assemblers[spot] = oldAssembler;
                    assemblers[spot].reset();
                } else {
                    assemblers[spot] = new FrameAssembler();
                }
            }
            return spot;
        }

        /**
         * Get the assembler used for given timestamp
         *
         * @param timeStamp
         * @return FrameAssembler associated to timeStamp
         */
        public int getAssembler(long timeStamp) {
            int assemblerNumber = -1;
            for (int i = 0; i < numberOfAssemblers; i++) {
                if (assemblers[i].getTimeStamp() == timeStamp) {
                    assemblerNumber = i;
                }
            }
            if (assemblerNumber == -1) {
                // Given timestamp never used, we create a new assembler
                assemblerNumber = createNewAssembler(timeStamp);
            }
            return assemblerNumber;
        }

        /**
         * Remove oldest FrameAssembler than given timeStamp (if given timeStamp
         * has been rendered, then oldest ones are no more of no use) This also
         * removes given timeStamp
         *
         * @param timeStamp
         */
        public void removeOldestThan(long timeStamp) {
            // Find spot from which to remove
            int spot = numberOfAssemblers - 1;
            for (int i = 0; i < numberOfAssemblers; i++) {
                if (timeStamp <= assemblers[i].getTimeStamp()) {
                    spot = i;
                }
            }
            // remove all assemblers with oldest timeStamp to the left
            for (int i = numberOfAssemblers - 1; i > spot; i--) {
                assemblers[i - 1] = assemblers[i];
            }
            numberOfAssemblers -= spot + 1;
        }
    }
}
