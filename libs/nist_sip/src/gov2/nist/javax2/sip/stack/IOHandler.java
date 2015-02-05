/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 United States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 *
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 *
 * .
 *
 */
/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).       *
 *******************************************************************************/
package gov2.nist.javax2.sip.stack;

import gov2.nist.core.StackLogger;
import gov2.nist.javax2.sip.SipStackImpl;
import gov2.nist.javax2.sip.header.ViaList;
import gov2.nist.javax2.sip.message.SIPMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;

import android.text.TextUtils;
import javax2.sip.InvalidArgumentException;
import javax2.sip.address.Address;
import javax2.sip.header.ContactHeader;

/*
 * TLS support Added by Daniel J.Martinez Manzano <dani@dif.um.es>
 * 
 */

/**
 * Low level Input output to a socket. Caches TCP connections and takes care of re-connecting to
 * the remote party if the other end drops the connection
 * 
 * @version 1.2
 * 
 * @author M. Ranganathan <br/>
 * 
 * 
 */

class IOHandler {

    private Semaphore ioSemaphore = new Semaphore(1);

    private SipStackImpl sipStack;

    private static String TCP = "tcp";

    // Added by Daniel J. Martinez Manzano <dani@dif.um.es>
    private static String TLS = "tls";

    // A cache of client sockets that can be re-used for
    // sending tcp messages.
    private ConcurrentHashMap<String, Socket> socketTable;

    protected static String makeKey(InetAddress addr, int port) {
        return addr.getHostAddress() + ":" + port;

    }

    protected IOHandler(SIPTransactionStack sipStack) {
        this.sipStack = (SipStackImpl) sipStack;
        this.socketTable = new ConcurrentHashMap<String, Socket>();

    }

    protected void putSocket(String key, Socket sock) {
        socketTable.put(key, sock);

    }

    protected Socket getSocket(String key) {
        return (Socket) socketTable.get(key);
    }

    protected void removeSocket(String key) {
        socketTable.remove(key);
    }

    /**
     * A private function to write things out. This needs to be synchronized as writes can occur
     * from multiple threads. We write in chunks to allow the other side to synchronize for large
     * sized writes.
     */
    // Changed by Deutsche Telekom
    // ***###*** DTAG, AS 2012-09-10; work around Android issue 34727 (large TCP packets from or to port 5060 not send)
    private void writeChunks(OutputStream outputStream, byte[] bytes, int length, boolean smallChunks)
            throws IOException {
        // Chunk size is 16K - this hack is for large
        // writes over slow connections.
        synchronized (outputStream) {
            // outputStream.write(bytes,0,length);
            // Changed by Deutsche Telekom
            // ***###*** DTAG, AS 2012-09-10; work around Android issue 34727 (large TCP packets from or to port 5060 not send)
            int chunksize = 8 * 1024;
            if (smallChunks) {
                chunksize = 512;
            }
            for (int p = 0; p < length; p += chunksize) {
                int chunk = p + chunksize < length ? chunksize : length - p;
                outputStream.write(bytes, p, chunk);
            }
        }
        outputStream.flush();
    }

    /**
     * Creates and binds, if necessary, a socket connected to the specified destination address
     * and port and then returns its local address.
     * 
     * @param dst the destination address that the socket would need to connect to.
     * @param dstPort the port number that the connection would be established with.
     * @param localAddress the address that we would like to bind on (null for the "any" address).
     * @param localPort the port that we'd like our socket to bind to (0 for a random port).
     * 
     * @return the SocketAddress that this handler would use when connecting to the specified
     *         destination address and port.
     * 
     * @throws IOException
     */
    public SocketAddress obtainLocalAddress(InetAddress dst, int dstPort,
            InetAddress localAddress, int localPort) throws IOException {
        String key = makeKey(dst, dstPort);

        Socket clientSock = getSocket(key);

        if (clientSock == null) {
            clientSock = sipStack.getNetworkLayer().createSocket(dst, dstPort, localAddress,
                    localPort);
            putSocket(key, clientSock);
        }

        return clientSock.getLocalSocketAddress();

    }

    /**
     * Send an array of bytes.
     * 
     * @param senderAddress -- inet src address
     * @param receiverAddress -- inet dst address
     * @param contactPort -- port to connect to.
     * @param transport -- tcp or udp.
     * @param message -- sip message to send
     * @param retry -- retry to connect if the other end closed connection
     * @param messageChannel -- message channel
     * @throws IOException -- if there is an IO exception sending message.
     */

    public Socket sendBytes(InetAddress senderAddress, InetAddress receiverAddress,
            int contactPort, String transport, SIPMessage message, boolean retry,
            MessageChannel messageChannel) throws IOException {
        int retry_count = 0;
        int max_retry = retry ? 2 : 1;
        // Server uses TCP transport. TCP client sockets are cached
        if (sipStack.isLoggingEnabled()) {
            sipStack.getStackLogger().logDebug(
                    "sendBytes " + transport + " inAddr " + receiverAddress.getHostAddress()
                            + " port = " + contactPort );
        }
        if (sipStack.isLoggingEnabled() && sipStack.isLogStackTraceOnMessageSend()) {
            sipStack.getStackLogger().logStackTrace(StackLogger.TRACE_INFO);
        }
        if (transport.compareToIgnoreCase(TCP) == 0) {
            String key = makeKey(receiverAddress, contactPort);
            // This should be in a synchronized block ( reported by
            // Jayashenkhar ( lucent ).

            try {
                boolean retval = this.ioSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS); 
                if (!retval) {
                    throw new IOException(
                            "Could not acquire IO Semaphore after 10 seconds -- giving up ");
                }
            } catch (InterruptedException ex) {
                throw new IOException("exception in acquiring sem");
            }
            Socket clientSock = getSocket(key);

            try {

                while (retry_count < max_retry) {
                    if (clientSock == null) {
                        if (sipStack.isLoggingEnabled()) {
                            sipStack.getStackLogger().logDebug("inaddr = " + receiverAddress);
                            sipStack.getStackLogger().logDebug("port = " + contactPort);
                        }
                        // note that the IP Address for stack may not be
                        // assigned.
                        // sender address is the address of the listening point.
                        // in version 1.1 all listening points have the same IP
                        // address (i.e. that of the stack). In version 1.2
                        // the IP address is on a per listening point basis.
                        clientSock = sipStack.getNetworkLayer().createSocket(receiverAddress,
                                contactPort, senderAddress);
                        OutputStream outputStream = clientSock.getOutputStream();
                        // Changed by Deutsche Telekom
                        // ***###*** DTAG, AS 2012-09-10; work around Android issue 34727 (large TCP packets from or to port 5060 not send)
                        boolean doIssue34727workarround = false;
                        if (clientSock.getLocalPort()==5060 || contactPort==5060)
                            doIssue34727workarround = true;
                        // Update Via header to reflect local port
                        updateViaHeaderPort(clientSock.getLocalPort(), clientSock.getLocalAddress(),
                            message);
                        // Update Contact header to reflect local port
                        updateContactHeaderPort(clientSock.getLocalPort(), message);
                        // Encode the SIP message into byte array
                        byte[] bytes = message.encodeAsBytes(transport);
                        
                        writeChunks(outputStream, bytes, bytes.length, doIssue34727workarround);
                        putSocket(key, clientSock);
                        break;
                    } else {
                        try {
                            OutputStream outputStream = clientSock.getOutputStream();
                            // Changed by Deutsche Telekom
                            // ***###*** DTAG, AS 2012-09-10; work around Android issue 34727 (large TCP packets from or to port 5060 not send)
                            boolean doIssue34727workarround = false;
                            if (clientSock.getLocalPort()==5060 || contactPort==5060)
                                doIssue34727workarround = true;
                            // Update Via header to reflect local port
                            updateViaHeaderPort(clientSock.getLocalPort(),
                                clientSock.getLocalAddress(), message);
                            // Update Contact header to reflect local port
                            updateContactHeaderPort(clientSock.getLocalPort(), message);
                            // Encode the SIP message into byte array
                            byte[] bytes = message.encodeAsBytes(transport);
                            
                            writeChunks(outputStream, bytes, bytes.length, doIssue34727workarround);
                            break;
                        } catch (IOException ex) {
                            if (sipStack.isLoggingEnabled())
                                sipStack.getStackLogger().logDebug(
                                        "IOException occured retryCount " + retry_count);
                            // old connection is bad.
                            // remove from our table.
                            removeSocket(key);
                            try {
                                clientSock.close();
                            } catch (Exception e) {
                            }
                            clientSock = null;
                            retry_count++;
                        }
                    }
                }
            } finally {
                ioSemaphore.release();
            }

            if (clientSock == null) {

                if (sipStack.isLoggingEnabled()) {
                    sipStack.getStackLogger().logDebug(this.socketTable.toString());
                    sipStack.getStackLogger().logError(
                            "Could not connect to " + receiverAddress + ":" + contactPort);
                }

                throw new IOException("Could not connect to " + receiverAddress + ":"
                        + contactPort);
            } else
                return clientSock;

            // Added by Daniel J. Martinez Manzano <dani@dif.um.es>
            // Copied and modified from the former section for TCP
        } else if (transport.compareToIgnoreCase(TLS) == 0) {
            String key = makeKey(receiverAddress, contactPort);
            try {
                boolean retval = this.ioSemaphore.tryAcquire(10000, TimeUnit.MILLISECONDS);
                if (!retval)
                    throw new IOException("Timeout acquiring IO SEM");
            } catch (InterruptedException ex) {
                throw new IOException("exception in acquiring sem");
            }
            Socket clientSock = getSocket(key);

            try {
                while (retry_count < max_retry) {
                    if (clientSock == null) {
                        if (sipStack.isLoggingEnabled()) {
                            sipStack.getStackLogger().logDebug("inaddr = " + receiverAddress);
                            sipStack.getStackLogger().logDebug("port = " + contactPort);
                        }

                        clientSock = sipStack.getNetworkLayer().createSSLSocket(receiverAddress,
                                contactPort, senderAddress);
                        SSLSocket sslsock = (SSLSocket) clientSock;
                        HandshakeCompletedListener listner = new HandshakeCompletedListenerImpl(
                                (TLSMessageChannel) messageChannel);
                        ((TLSMessageChannel) messageChannel)
                                .setHandshakeCompletedListener(listner);
                        sslsock.addHandshakeCompletedListener(listner);
                        sslsock.setEnabledProtocols(sipStack.getEnabledProtocols());
                        sslsock.startHandshake();

                        // Changed by Deutsche Telekom
                        // ***###*** DTAG, AS 2012-09-10; work around Android issue 34727 (large TCP packets from or to port 5060 not send)
                        boolean doIssue34727workarround = false;
                        if (clientSock.getLocalPort()==5060 || contactPort==5060)
                            doIssue34727workarround = true;
                        OutputStream outputStream = clientSock.getOutputStream();
                        // Update Via header to reflect local port
                        updateViaHeaderPort(clientSock.getLocalPort(), clientSock.getLocalAddress(),
                            message);
                        // Update Contact header to reflect local port
                        updateContactHeaderPort(clientSock.getLocalPort(), message);
                        // Encode the SIP message into byte array
                        byte[] bytes = message.encodeAsBytes(transport);
                        
                        writeChunks(outputStream, bytes, bytes.length, doIssue34727workarround);
                        putSocket(key, clientSock);
                        break;
                    } else {
                        try {
                            // Changed by Deutsche Telekom
                            // ***###*** DTAG, AS 2012-09-10; work around Android issue 34727 (large TCP packets from or to port 5060 not send)
                            boolean doIssue34727workarround = false;
                            if (clientSock.getLocalPort()==5060 || contactPort==5060)
                                doIssue34727workarround = true;
                            OutputStream outputStream = clientSock.getOutputStream();
                            // Update Via header to reflect local port
                            updateViaHeaderPort(clientSock.getLocalPort(),
                                clientSock.getLocalAddress(), message);
                            // Update Contact header to reflect local port
                            updateContactHeaderPort(clientSock.getLocalPort(), message);
                            // Encode the SIP message into byte array
                            byte[] bytes = message.encodeAsBytes(transport);
                            
                            writeChunks(outputStream, bytes, bytes.length, doIssue34727workarround);
                            break;
                        } catch (IOException ex) {
                            if (sipStack.isLoggingEnabled())
                                sipStack.getStackLogger().logException(ex);
                            // old connection is bad.
                            // remove from our table.
                            removeSocket(key);
                            try {
                                clientSock.close();
                            } catch (Exception e) {
                            }
                            clientSock = null;
                            retry_count++;
                        }
                    }
                }
            } finally {
                ioSemaphore.release();
            }
            if (clientSock == null) {
                throw new IOException("Could not connect to " + receiverAddress + ":"
                        + contactPort);
            } else
                return clientSock;

        } else {
            // This is a UDP transport...
            DatagramSocket datagramSock = sipStack.getNetworkLayer().createDatagramSocket();
            datagramSock.connect(receiverAddress, contactPort);
            
            // Update Via header to reflect local port
            updateViaHeaderPort(datagramSock.getLocalPort(), datagramSock.getLocalAddress(),
                    message);
            // Update Contact header to reflect local port
            updateContactHeaderPort(datagramSock.getLocalPort(), message);
            // Encode the SIP message into byte array
            byte[] bytes = message.encodeAsBytes(transport);
            
            DatagramPacket dgPacket = new DatagramPacket(bytes, 0, bytes.length, receiverAddress,
                    contactPort);
            datagramSock.send(dgPacket);
            datagramSock.close();
            return null;
        }

    }
    
	/**
	 * Update port of Via header to reflect local port
	 * 
	 * @param localPort the local port
     * @param localAddress the local address
     * @param message the SIP message to be updated
     */
    private void updateViaHeaderPort(int localPort, InetAddress localAddress, SIPMessage message) {
		if (localAddress == null) {
			return;
		}
		if (message == null || message.getViaHeaders() == null) {
			return;
		}
		ViaList viaList = message.getViaHeaders();
		if (viaList == null || viaList.isEmpty()) {
			return;
		}
		try {
			String localHostAddress = localAddress.getHostAddress();
			String viaHostAddress = viaList.get(0).getHost();
			// Only update port of via header if address of via header is set to the local host address
			if (!TextUtils.isEmpty(viaHostAddress) && viaHostAddress.equals(localHostAddress)) {
				viaList.get(0).setPort(localPort);
			}
		} catch (InvalidArgumentException e) {
			if (sipStack.isLoggingEnabled()) {
				sipStack.getStackLogger().logError(e.getMessage(), e);
			}
		}
    }
    
    /**
     * Update port of Contact header to reflect local port
     *
     * @param localPort the local port
     * @param message the SIP message to be updated
     */
    private void updateContactHeaderPort(int localPort, SIPMessage message) {
        if (message != null && message.getContactHeader() != null) {
            ContactHeader contactHeader = message.getContactHeader();
            Address contactAddress = contactHeader.getAddress();
            contactAddress.setPort(localPort);
        }
    }

    /**
     * Close all the cached connections.
     */
    public void closeAll() {
        for (Enumeration<Socket> values = socketTable.elements(); values.hasMoreElements();) {
            Socket s = (Socket) values.nextElement();
            try {
                s.close();
            } catch (IOException ex) {
            }
        }

    }

}
