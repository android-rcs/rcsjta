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

/**
 * Class Processor.
 */
public class Processor extends Thread {
    /**
     * Creates a new instance of Processor.
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3 array.
     */
    public Processor(org.gsma.rcs.core.ims.protocol.rtp.stream.ProcessorInputStream arg1, org.gsma.rcs.core.ims.protocol.rtp.stream.ProcessorOutputStream arg2, org.gsma.rcs.core.ims.protocol.rtp.codec.Codec[] arg3) {
        super();
    }

    public void run() {

    }

    /**
     * Returns the input stream.
     *  
     * @return  The input stream.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.stream.ProcessorInputStream getInputStream() {
        return (org.gsma.rcs.core.ims.protocol.rtp.stream.ProcessorInputStream) null;
    }

    /**
     * Returns the output stream.
     *  
     * @return  The output stream.
     */
    public org.gsma.rcs.core.ims.protocol.rtp.stream.ProcessorOutputStream getOutputStream() {
        return (org.gsma.rcs.core.ims.protocol.rtp.stream.ProcessorOutputStream) null;
    }

    public void startProcessing() {

    }

    public void stopProcessing() {

    }

} // end Processor
