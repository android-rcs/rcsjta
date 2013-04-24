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

package com.orangelabs.rcs.core.ims.protocol.msrp;

import java.util.Vector;

/**
 * Fifo buffer
 * 
 * @author JM. Auffret
 */
public class FifoBuffer {
	/**
	 * Number of messages in the buffer
	 */
	private int numMessage = 0;

	/**
	 * Buffer of messages
	 */
	private Vector<Object> fifo = new Vector<Object>();

	/**
	 * Add a message in the buffer
	 *
	 * @param obj Message
	 */
	public synchronized void putMessage(Object obj) {
		fifo.addElement(obj);
		numMessage++;
		notifyAll();
	}

	/**
	 * Read a message in the buffer. This is a blocking method until a
	 * message is received in the buffer.
	 * 
	 * @return Message
	 */
	public synchronized Object getMessage() {
		Object message = null;
		if (numMessage == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				// Nothing to do
			}
		}
		if (numMessage != 0) {
			message = fifo.elementAt(0);
			fifo.removeElementAt(0);
			numMessage--;
			notifyAll();
		}
		return message;
	}

	/**
	 * Read a message in the buffer. This is a blocking method until a timeout
	 * occurs or a message is received in the buffer.
	 * 
	 * @param timeout Timeout
	 * @return Message
	 */
	public synchronized Object getMessage(int timeout) {
		Object message = null;
		if (numMessage == 0) {
			try {
				wait(timeout);
			} catch (InterruptedException e) {
				// Nothing to do
			}
		}
		if (numMessage != 0) {
			message = fifo.elementAt(0);
			fifo.removeElementAt(0);
			numMessage--;
			notifyAll();
		}
		return message;
	}

	/**
	 * Unblock the reading
	 */
	public void unblockRead() {
		synchronized (this) {
			this.notifyAll();
		}
	}
}
