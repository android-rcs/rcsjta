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

package org.gsma.rcs.core.ims.protocol.rtp.codec.video.h264;

/**
 * Class JavaDepacketizer.
 */
public class JavaDepacketizer extends org.gsma.rcs.core.ims.protocol.rtp.codec.video.VideoCodec {
    /**
     * Creates a new instance of JavaDepacketizer.
     */
    public JavaDepacketizer() {
        super();
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The int.
     */
    public int process(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1, org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg2) {
        return 0;
    }

    /**
     * Class FrameAssembler.
     */
    public static class FrameAssembler {
        /**
         * Creates a new instance of FrameAssembler.
         */
        public FrameAssembler() {

        }

        /**
         *  
         * @param arg1 The arg1.
         */
        public void put(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1) {

        }

        /**
         *  
         * @return  The boolean.
         */
        public boolean complete() {
            return false;
        }

        /**
         * Returns the time stamp.
         *  
         * @return  The time stamp.
         */
        public long getTimeStamp() {
            return 0l;
        }

    } // end FrameAssembler

    /**
     * Class FrameAssemblerCollection.
     */
    public static class FrameAssemblerCollection {
        /**
         * Creates a new instance of FrameAssemblerCollection.
         */
        public FrameAssemblerCollection() {

        }

        /**
         *  
         * @param arg1 The arg1.
         */
        public void put(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1) {

        }

        /**
         * Returns the assembler.
         *  
         * @param arg1 The arg1.
         * @return  The assembler.
         */
        public int getAssembler(long arg1) {
            return 0;
        }

        /**
         * Returns the last active assembler.
         *  
         * @return  The last active assembler.
         */
        public JavaDepacketizer.FrameAssembler getLastActiveAssembler() {
            return (JavaDepacketizer.FrameAssembler) null;
        }

        /**
         * Creates the new assembler.
         *  
         * @param arg1 The arg1.
         * @return  The int.
         */
        public int createNewAssembler(long arg1) {
            return 0;
        }

        /**
         * Removes a oldest than.
         *  
         * @param arg1 The arg1.
         */
        public void removeOldestThan(long arg1) {

        }

    } // end FrameAssemblerCollection

} // end JavaDepacketizer
