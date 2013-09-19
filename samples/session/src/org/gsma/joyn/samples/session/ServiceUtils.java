package org.gsma.joyn.samples.session;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Enumeration;

import org.gsma.joyn.capability.CapabilityService;
import org.gsma.joyn.samples.session.sdp.SdpUtils;

public class ServiceUtils {
	/**
	 * CRLF constant
	 */
	public final static String CRLF = "\r\n";

	/**
	 * Service ID constant
	 */
	public final static String SERVICE_ID = CapabilityService.EXTENSION_BASE_NAME + "=\"" +
			CapabilityService.EXTENSION_PREFIX_NAME + ".orange.texto\"";

	/**
	 * Construct an NTP time from a date in milliseconds
	 *
	 * @param date Date in milliseconds
	 * @return NTP time in string format
	 */
	public static String constructNTPtime(long date) {
		long ntpTime = 2208988800L;
		long startTime = (date / 1000) + ntpTime;
		return String.valueOf(startTime);
	}

	/**
	 * Returns the local IP address
	 *
	 * @return IP address
	 */
	public static String getLocalIpAddress() {
		try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = (NetworkInterface)en.nextElement();
	            for (Enumeration<InetAddress> addr = intf.getInetAddresses(); addr.hasMoreElements();) {
	                InetAddress inetAddress = (InetAddress)addr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
	            }
	        }
	        return null;
		} catch(Exception e) {
			return null;
		}
	}

	/**
	 * Returns local SDP
	 * 
	 * @param mode Connection mode (active or passive)
	 * @param port Local port
	 * @return SDP
	 */
	public static String getLocalSdp(String mode, int port) {
		String ntpTime = ServiceUtils.constructNTPtime(System.currentTimeMillis());
		String ipAddress = ServiceUtils.getLocalIpAddress();
		// Test MSRP
		String sdp =
    		"v=0" + "\r\n" +
            "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + CRLF +
            "s=DEMO joyn API" + CRLF +
			"c=" + SdpUtils.formatAddressType(ipAddress) + CRLF +
            "t=0 0" + CRLF +
            "m=message " + port + " TCP/MSRP *" + CRLF +
            "a=path:" + "msrp://" + ipAddress + ":" + port + "/" + System.currentTimeMillis() + ";tcp" + CRLF +
            "a=setup:" + mode + CRLF +
			"a=sendrecv" + CRLF;
/*		// Test RTP audio
 		String sdp =
	    		"v=0" + "\r\n" +
	            "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + CRLF +
	            "s=DEMO joyn API" + CRLF +
				"c=" + SdpUtils.formatAddressType(ipAddress) + CRLF +
	            "t=0 0" + CRLF +
	            "m=audio 5000 RTP/AVP 96" + CRLF +
	            "a=rtpmap:96 AMR" + CRLF +            
				"a=sendrecv" + CRLF;*/
		return sdp;
	}

    /**
     * Generate a free TCP port number
     *
     * @param portBase TCP port base
     * @return Local TCP port
     */
    public static int generateLocalTcpPort(int portBase) {
    	int resp = -1;
		int port = portBase;
		while(resp == -1) {
			if (isLocalTcpPortFree(port)) {
				// Free TCP port found
				resp = port;
			} else {
				port++;
			}
		}
    	return resp;
    }
    
	/**
     * Test if the given local TCP port is really free (not used by
     * other applications)
     *
     * @param port Port to check
     * @return Boolean
     */
    private static boolean isLocalTcpPortFree(int port) {
    	boolean res = false;
    	try {
    		ServerSocket socket = new ServerSocket(port);
    		socket.close();
    		res = true;
    	} catch(IOException e) {
    		res = false;
    	}
    	return res;
    }    
}
