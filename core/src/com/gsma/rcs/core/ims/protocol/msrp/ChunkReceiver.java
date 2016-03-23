/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.protocol.msrp;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.utils.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * Chunks receiver
 * 
 * @author jexa7410
 */
public class ChunkReceiver extends Thread {
    /**
     * MSRP connection
     */
    private MsrpConnection mConnection;

    /**
     * MSRP input stream
     */
    private InputStream mStream;

    /**
     * Termination flag
     */
    private boolean mTerminated;

    /**
     * Maximum length of MSRP chunk buffer
     */
    private int mBufferLength = MsrpConstants.CHUNK_MAX_SIZE;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(ChunkReceiver.class.getName());

    /**
     * Constructor
     * 
     * @param connection MSRP connection
     * @param stream TCP input stream
     */
    public ChunkReceiver(MsrpConnection connection, InputStream stream) {
        mConnection = connection;
        mStream = stream;
    }

    /**
     * Returns the MSRP connection
     * 
     * @return MSRP connection
     */
    public MsrpConnection getConnection() {
        return mConnection;
    }

    /**
     * Terminate the receiver
     */
    public void terminate() {
        mTerminated = true;
        interrupt();
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            boolean msrpTraceEnabled = MsrpConnection.isMsrpTraceEnabled();
            // Background processing
            while (!mTerminated) {
                StringBuilder trace = new StringBuilder();

                // Read first line of a new data chunk
                StringBuilder line = readLine();

                if (line.length() == 0) {
                    if (msrpTraceEnabled) {
                        System.out.println("<<< End of stream");
                    }
                    return;
                }

                if (msrpTraceEnabled) {
                    trace.append(line);
                    trace.append(MsrpConstants.NEW_LINE);
                }
                // Check the MSRP tag
                String[] firstLineTags = line.toString().split(" ");
                if ((firstLineTags.length < 3)
                        || !firstLineTags[0].equals(MsrpConstants.MSRP_HEADER)) {
                    if (msrpTraceEnabled) {
                        System.out.println("<<< Not a MSRP message");
                    }
                    return;
                }

                // Get the transaction ID from the first line
                String txId = firstLineTags[1];
                String end = MsrpConstants.END_MSRP_MSG + txId;

                // Get response code or method name from the first line
                int responseCode = -1;
                String method = null;
                try {
                    responseCode = Integer.parseInt(firstLineTags[2]);
                } catch (NumberFormatException e) {
                    method = firstLineTags[2];
                }

                // Data chunk
                byte[] data = null;

                // Read next lines
                Hashtable<String, String> headers = new Hashtable<String, String>();
                char continuationFlag = '\0';
                int totalSize = 0;
                while (continuationFlag == '\0' && !mTerminated) {
                    line = readLine();
                    if (msrpTraceEnabled) {
                        trace.append(line);
                        trace.append(MsrpConstants.NEW_LINE);
                    }

                    // Test if there is a new line separating headers from the data
                    if (line.length() == 0) {
                        // Read data
                        String byteRange = headers.get(MsrpConstants.HEADER_BYTE_RANGE);
                        int chunkSize = -1;
                        if (byteRange != null) {
                            chunkSize = MsrpUtils.getChunkSize(byteRange);
                            totalSize = MsrpUtils.getTotalSize(byteRange);

                            // Changed by Deutsche Telekom
                            if (chunkSize == 0) {
                                mBufferLength = totalSize;
                            }
                        }

                        if (chunkSize >= 0) {
                            // Use Byte-Range value to read directly the block of data
                            byte[] buffer = readChunkedData(chunkSize, end);

                            if (chunkSize > 0) {
                                data = buffer;
                                // TODO: we could harden the code by checking whether the chunk was
                                // shorter than expected
                            } else {
                                // Cut off continuation flag
                                data = new byte[buffer.length - 1];
                                System.arraycopy(buffer, 0, data, 0, buffer.length - 1);
                                continuationFlag = (char) buffer[buffer.length - 1];
                            }

                            if (msrpTraceEnabled) {
                                trace.append(new String(data, UTF8));
                                trace.append(MsrpConstants.NEW_LINE);
                            }
                        } else {
                            // Read until terminating header is found
                            StringBuilder buffer = new StringBuilder();
                            StringBuilder dataline;
                            boolean endchunk = false;
                            while ((!endchunk) && (buffer.length() < MsrpConstants.CHUNK_MAX_SIZE)) {
                                dataline = readLine();
                                if ((dataline.length() - 1 == end.length())
                                        && (dataline.toString().startsWith(end))) {
                                    continuationFlag = dataline.charAt(dataline.length() - 1);
                                    endchunk = true;
                                } else {
                                    if (buffer.length() > 0) {
                                        buffer.append(MsrpConstants.NEW_LINE);
                                    }
                                    buffer.append(dataline);
                                }
                            }
                            data = buffer.toString().getBytes(UTF8);
                            totalSize = data.length;

                            if (msrpTraceEnabled) {
                                trace.append(new String(data, UTF8));
                                trace.append(MsrpConstants.NEW_LINE);
                                trace.append(end);
                                trace.append(continuationFlag);
                            }
                        }
                    } else if (line.toString().startsWith(end)) {
                        continuationFlag = line.charAt(line.length() - 1);
                    } else {
                        // It's an header
                        int index = line.indexOf(":");
                        String headerName = line.substring(0, index).trim();
                        String headerValue = line.substring(index + 1).trim();

                        // Add the header in the list
                        headers.put(headerName, headerValue);
                    }
                }

                final MsrpSession session = mConnection.getSession();
                // Process the received MSRP message
                if (responseCode != -1) {
                    // Process MSRP response
                    if (msrpTraceEnabled) {
                        System.out.println("<<< Receive MSRP response:\n" + trace);
                    }
                    session.receiveMsrpResponse(responseCode, txId, headers);
                } else {
                    // Process MSRP request
                    if (MsrpConstants.METHOD_SEND.equals(method)) {
                        // Process a SEND request
                        if (msrpTraceEnabled) {
                            System.out.println("<<< Receive MSRP SEND request:\n" + trace);
                        }
                        session.receiveMsrpSend(txId, headers, continuationFlag, data, totalSize);
                    } else if (MsrpConstants.METHOD_REPORT.equals(method)) {
                        // Process a REPORT request
                        if (msrpTraceEnabled) {
                            System.out.println("<<< Receive MSRP REPORT request:\n" + trace);
                        }
                        session.receiveMsrpReport(txId, headers);
                    } else {
                        // Unknown request
                        if (msrpTraceEnabled) {
                            System.out.println("<<< Unknown request received:\n" + trace);
                        }
                        // Remove transaction info from list
                        // Changed by Deutsche Telekom
                        session.removeMsrpTransactionInfo(txId);
                    }
                }

                // Check transaction info data
                // Changed by Deutsche Telekom
                session.checkMsrpTransactionInfo();
            }
        } catch (FileAccessException e) {
            sLogger.error("Unable to receive chunks!", e);
            if (!mTerminated) {
                /* Notify the session listener that an error has occured */
                final MsrpSession session = mConnection.getSession();
                session.getMsrpEventListener().msrpTransferError(null, e.getMessage(),
                        TypeMsrpChunk.Unknown);

                /* Check transaction info data */
                session.checkMsrpTransactionInfo();
                mTerminated = true;
            }
        } catch (ContactManagerException e) {
            sLogger.error("Unable to receive chunks!", e);
            if (!mTerminated) {
                /* Notify the session listener that an error has occured */
                final MsrpSession session = mConnection.getSession();
                session.getMsrpEventListener().msrpTransferError(null, e.getMessage(),
                        TypeMsrpChunk.Unknown);

                /* Check transaction info data */
                session.checkMsrpTransactionInfo();
                mTerminated = true;
            }
        } catch (PayloadException e) {
            sLogger.error("Unable to receive chunks!", e);
            if (!mTerminated) {
                /* Notify the session listener that an error has occured */
                final MsrpSession session = mConnection.getSession();
                session.getMsrpEventListener().msrpTransferError(null, e.getMessage(),
                        TypeMsrpChunk.Unknown);

                /* Check transaction info data */
                session.checkMsrpTransactionInfo();
                mTerminated = true;
            }
        } catch (NetworkException e) {
            if (sLogger.isActivated()) {
                sLogger.debug(e.getMessage());
            }
            if (!mTerminated) {
                /* Notify the session listener that an error has occured */
                /* Changed by Deutsche Telekom */
                final MsrpSession session = mConnection.getSession();
                session.getMsrpEventListener().msrpTransferError(null, e.getMessage(),
                        TypeMsrpChunk.Unknown);

                /* Check transaction info data */
                /* Changed by Deutsche Telekom */
                session.checkMsrpTransactionInfo();
                mTerminated = true;
            }
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Unable to receive chunks!", e);
            if (!mTerminated) {
                /* Notify the session listener that an error has occured */
                final MsrpSession session = mConnection.getSession();
                session.getMsrpEventListener().msrpTransferError(null, e.getMessage(),
                        TypeMsrpChunk.Unknown);

                /* Check transaction info data */
                session.checkMsrpTransactionInfo();
                mTerminated = true;
            }
        }
    }

    /**
     * Read line
     * 
     * @return String
     * @throws NetworkException
     */
    private StringBuilder readLine() throws NetworkException {
        try {
            StringBuilder line = new StringBuilder();
            int previous = -1;
            int current = -1;
            while ((current = mStream.read()) != -1) {
                line.append((char) current);
                if ((previous == MsrpConstants.CHAR_LF) && (current == MsrpConstants.CHAR_CR)) {
                    return line.delete(line.length() - 2, line.length());
                }
                previous = current;
            }
            return line;
        } catch (IOException e) {
            throw new NetworkException("Failed to read line!", e);
        }
    }

    /**
     * Read chunked data
     * 
     * @param chunkSize Chunk size
     * @return Data
     * @throws NetworkException
     */
    private byte[] readChunkedData(int chunkSize, String endTag) throws NetworkException {
        try {
            // Read data until chunk size is reached
            byte[] result = null;
            if (chunkSize != 0) {
                result = new byte[chunkSize];
                int nbRead = 0;
                int nbData = -1;
                while ((nbRead < chunkSize)
                        && ((nbData = mStream.read(result, nbRead, chunkSize - nbRead)) != -1)) {
                    nbRead += nbData;
                }
            } else {
                int b;
                int tagLength = endTag.length();
                int[] tail = new int[tagLength];
                byte[] buffer = new byte[mBufferLength + tagLength + 2];

                // MSRP end tag in reverse order
                int[] match = new int[tagLength];
                for (int i = 0; i < tagLength; i++) {
                    match[i] = endTag.charAt(tagLength - i - 1);
                }

                // Read stream byte by byte
                for (int j = 0; (b = mStream.read()) != -1; j++) {
                    // Sliding window over last received bytes
                    System.arraycopy(tail, 0, tail, 1, tagLength - 1);
                    tail[0] = b;

                    if (b != match[0]) {
                        buffer[j] = (byte) b;
                    } else {
                        // First char matches; let's check for the others
                        boolean tagFound = true;
                        for (int k = 1; k < tagLength - 1; k++) {
                            if (tail[k] != match[k]) {
                                buffer[j] = (byte) b;
                                tagFound = false;
                                break;
                            }
                        }
                        if (tagFound) {
                            // Strip off MSRP end tag
                            // j+1 characters read; remove tagLength characters and CR/LF plus one
                            // extra
                            // character for continuation flag
                            result = new byte[j - tagLength];
                            System.arraycopy(buffer, 0, result, 0, j - tagLength - 1); // remove tag
                                                                                       // and
                                                                                       // CR/LF

                            // read continuation flag
                            result[j - tagLength - 1] = (byte) mStream.read();
                            break;
                        }
                    }
                }
            }
            mStream.read(); // Read LF
            mStream.read(); // Read CR
            return result;
        } catch (IOException e) {
            throw new NetworkException("Failed to read chunk data!", e);
        }
    }
}
