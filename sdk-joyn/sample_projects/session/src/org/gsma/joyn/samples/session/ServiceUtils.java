package org.gsma.joyn.samples.session;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.gsma.joyn.capability.CapabilityService;

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
	 * @return SDP
	 */
	public static String getLocalSdp(String mode) {
		String ntpTime = ServiceUtils.constructNTPtime(System.currentTimeMillis());
		String myIpAddress = ServiceUtils.getLocalIpAddress();
		String sdp =
    		"v=0" + "\r\n" +
            "o=- " + ntpTime + " " + ntpTime + " IN IP4 " + myIpAddress + CRLF +
            "s=DEMO" + CRLF +
			"c=IN IP4 " + myIpAddress + CRLF +
            "t=0 0" + CRLF +
            "m=text 5000 TCP 96" + CRLF + 
            "a=setup:" + mode + CRLF +
			"a=sendrecv" + CRLF;
		return sdp;
	}
}
