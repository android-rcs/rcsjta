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

package org.gsma.joyn.samples.session;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Socket server connection
 * 
 * @author Jean-Marc AUFFRET
 */
public class TcpServerConnection {
	/**
	 * Socket server connection
	 */
	private ServerSocket acceptSocket = null; 

    /**
	 * Constructor
	 */
	public TcpServerConnection() {
	}

	/**
	 * Open the socket
	 * 
	 * @param port Local port
	 * @throws IOException
	 */
	public void open(int port) throws IOException {
		acceptSocket = new ServerSocket(port);
	}

	/**
	 * Close the socket
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (acceptSocket != null) {
			acceptSocket.close();
			acceptSocket = null;		
		}
	}
	
	/**
	 * Accept connection
	 * 
	 * @return Socket connection
	 * @throws IOException
	 */
	public TcpClientConnection acceptConnection() throws IOException {
		if (acceptSocket != null) { 
			Socket socket = acceptSocket.accept();		
			return new TcpClientConnection(socket);
		} else {
			throw new IOException("Connection not openned");
		}
	}
}
