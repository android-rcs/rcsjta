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

package org.gsma.rcs.core.ims.protocol.rtp.codec;

/**
 * Class Codec.
 */
public abstract class Codec {
    /**
     * Constant BUFFER_PROCESSED_OK.
     */
    public static final int BUFFER_PROCESSED_OK = 0;

    /**
     * Constant BUFFER_PROCESSED_FAILED.
     */
    public static final int BUFFER_PROCESSED_FAILED = 1;

    /**
     * Constant INPUT_BUFFER_NOT_CONSUMED.
     */
    public static final int INPUT_BUFFER_NOT_CONSUMED = 2;

    /**
     * Constant OUTPUT_BUFFER_NOT_FILLED.
     */
    public static final int OUTPUT_BUFFER_NOT_FILLED = 4;

    /**
     * Creates a new instance of Codec.
     */
    public Codec() {

    }

    public void close() {

    }

    public void reset() {

    }

    public void open() {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The int.
     */
    public abstract int process(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1, org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg2);

    /**
     * Sets the input format.
     *  
     * @param arg1 The input format.
     * @return  The format.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.format.Format setInputFormat(org.gsma.rcs.core.ims.protocol.rtp.format.Format arg1) {
        return (org.gsma.rcs.core.ims.protocol.rtp.format.Format) null;
    }

    /**
     * Sets the output format.
     *  
     * @param arg1 The output format.
     * @return  The format.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.format.Format setOutputFormat(org.gsma.rcs.core.ims.protocol.rtp.format.Format arg1) {
        return (org.gsma.rcs.core.ims.protocol.rtp.format.Format) null;
    }

    /**
     * Returns the input format.
     *  
     * @return  The input format.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.format.Format getInputFormat() {
        return (org.gsma.rcs.core.ims.protocol.rtp.format.Format) null;
    }

    /**
     * Returns the output format.
     *  
     * @return  The output format.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.format.Format getOutputFormat() {
        return (org.gsma.rcs.core.ims.protocol.rtp.format.Format) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @return  The boolean.
     */
    protected boolean isEOM(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1) {
        return false;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    protected void propagateEOM(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @param arg4 The arg4.
     */
    protected void updateOutput(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1, org.gsma.rcs.core.ims.protocol.rtp.format.Format arg2, int arg3, int arg4) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The byte array.
     */
    protected byte[] validateByteArraySize(org.gsma.rcs.core.ims.protocol.rtp.util.Buffer arg1, int arg2) {
        return (byte []) null;
    }

} // end Codec
