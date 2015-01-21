/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.core.ims.protocol.msrp;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.io.IOException;
import java.io.OutputStream;

import com.orangelabs.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chunks sender
 *
 * @author jexa7410
 */
public class ChunkSender extends Thread {
	/**
	 * MSRP connection
	 */
	private MsrpConnection connection;

	/**
	 * MSRP output stream
	 */
	private OutputStream stream;

	/**
	 * Buffer of chunks
	 */
	private FifoBuffer buffer = new FifoBuffer();

	/**
	 * Termination flag
	 */
	private boolean terminated = false;

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 *
	 * @param connection MSRP connection
	 * @param stream TCP output stream
	 */
	public ChunkSender(MsrpConnection connection, OutputStream stream) {
		this.connection = connection;
		this.stream = stream;
	}

	/**
	 * Returns the MSRP connection
	 *
	 * @return MSRP connection
	 */
	public MsrpConnection getConnection() {
		return connection;
	}

	/**
	 * Terminate the sender
	 */
	public void terminate() {
		terminated = true;
		buffer.unblockRead();
		try {
			interrupt();
		} catch(Exception e) {}
		if (logger.isActivated()) {
			logger.debug("Sender is terminated");
		}
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
			if (logger.isActivated()) {
				logger.debug("Sender is started");
			}

			// Read chunk to be sent
			byte chunk[] = null;
			while ((chunk = (byte[])buffer.getMessage()) != null) {
				// Write chunk to the output stream
				if (MsrpConnection.MSRP_TRACE_ENABLED) {
					System.out.println(">>> Send MSRP message:\n"
							+ new String(chunk, UTF8));
				}
				writeData(chunk);
			}
		} catch (Exception e) {
			if (terminated) {
				if (logger.isActivated()) {
					logger.debug("Chunk sender thread terminated");
				}
			} else {
				if (logger.isActivated()) {
					logger.error("Chunk sender has failed", e);
				}

				// Notify the msrp session listener that an error has occured
				// Changed by Deutsche Telekom
				connection.getSession().getMsrpEventListener().msrpTransferError(null, e.getMessage(), TypeMsrpChunk.Unknown);
			}
		}
	}

	/**
	 * Send a chunk
	 *
	 * @param chunk New chunk
	 * @throws IOException
	 */
	public void sendChunk(byte chunk[]) throws IOException {
		if (connection.getSession().isFailureReportRequested()) {
			buffer.putMessage(chunk);
		} else {
			sendChunkImmediately(chunk);
		}
	}

	/**
	 * Send a chunk immediately
	 *
	 * @param chunk New chunk
	 * @throws IOException
	 */
	public void sendChunkImmediately(byte chunk[]) throws IOException {
		if (MsrpConnection.MSRP_TRACE_ENABLED) {
			System.out.println(">>> Send MSRP message:\n"
					+ new String(chunk, UTF8));
		}
		writeData(chunk);
	}

	/**
	 * Write data to the stream
	 *
	 * @param chunk Data chunk
	 * @throws IOException
	 */
	private synchronized void writeData(byte chunk[]) throws IOException {
		stream.write(chunk);
		stream.flush();
	}
}
