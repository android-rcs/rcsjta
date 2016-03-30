/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.dns;

import com.gsma.rcs.utils.logger.Logger;

import android.test.AndroidTestCase;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax2.sip.ListeningPoint;

public class DnsTest extends AndroidTestCase {

    private static Logger sLogger = Logger.getLogger(DnsTest.class.getName());

    public void testDnsLib() {
        String domain = "rcs.lannion.com";
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("DNS NAPTR lookup for " + domain);
            }
            Lookup lookup = new Lookup(domain, Type.NAPTR);
            lookup.setCache(null);
            lookup.run();
            int code = lookup.getResult();
            if (code != Lookup.SUCCESSFUL) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Lookup error: " + code + "/" + lookup.getErrorString());
                }
            }
        } catch (TextParseException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Not a valid DNS name");
            }
        } catch (IllegalArgumentException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Not a valid DNS type");
            }
        }
    }

    public void testDnsResolution() {
        resolveImsProxyConfiguration("rcs.lannion.com", ListeningPoint.TCP);
    }

    private void resolveImsProxyConfiguration(String imsProxyAddr, String imsProxyProtocol) {
        int imsProxyPort = -1;

        // First try to resolve via a NAPTR query, then a SRV
        // query and finally via A query
        if (sLogger.isActivated()) {
            sLogger.debug("Resolve IMS proxy address...");
        }
        String ipAddress = null;

        // DNS NAPTR lookup
        String service = null;
        if (imsProxyProtocol.equalsIgnoreCase(ListeningPoint.UDP)) {
            service = "SIP+D2U";
        } else if (imsProxyProtocol.equalsIgnoreCase(ListeningPoint.TCP)) {
            service = "SIP+D2T";
        } else if (imsProxyProtocol.equalsIgnoreCase(ListeningPoint.TLS)) {
            service = "SIPS+D2T";
        }
        Record[] naptrRecords = getDnsNAPTR(imsProxyAddr);
        if ((naptrRecords != null) && (naptrRecords.length > 0)) {
            if (sLogger.isActivated()) {
                sLogger.debug("NAPTR records found: " + naptrRecords.length);
            }
            for (Record naptrRecord : naptrRecords) {
                NAPTRRecord naptr = (NAPTRRecord) naptrRecord;
                if (sLogger.isActivated()) {
                    sLogger.debug("NAPTR record: " + naptr.toString());
                }
                if ((naptr != null) && naptr.getService().equalsIgnoreCase(service)) {
                    // DNS SRV lookup
                    Record[] srvRecords = getDnsSRV(naptr.getReplacement().toString());
                    if ((srvRecords != null) && (srvRecords.length > 0)) {
                        SRVRecord srvRecord = getBestDnsSRV(srvRecords);
                        ipAddress = getDnsA(srvRecord.getTarget().toString());
                        imsProxyPort = srvRecord.getPort();
                    } else {
                        // Direct DNS A lookup
                        ipAddress = getDnsA(imsProxyAddr);
                    }
                }
            }
        } else {
            // Direct DNS SRV lookup
            if (sLogger.isActivated()) {
                sLogger.debug("No NAPTR record found: use DNS SRV instead");
            }
            String query;
            if (imsProxyAddr.startsWith("_sip.")) {
                query = imsProxyAddr;
            } else {
                query = "_sip._" + imsProxyProtocol.toLowerCase() + "." + imsProxyAddr;
            }
            Record[] srvRecords = getDnsSRV(query);
            if ((srvRecords != null) && (srvRecords.length > 0)) {
                SRVRecord srvRecord = getBestDnsSRV(srvRecords);
                ipAddress = getDnsA(srvRecord.getTarget().toString());
                imsProxyPort = srvRecord.getPort();
            } else {
                // Direct DNS A lookup
                if (sLogger.isActivated()) {
                    sLogger.debug("No SRV record found: use DNS A instead");
                }
                ipAddress = getDnsA(imsProxyAddr);
            }
        }

        imsProxyAddr = ipAddress;

        if (sLogger.isActivated()) {
            sLogger.debug("SIP outbound proxy configuration: " + imsProxyAddr + ":" + imsProxyPort
                    + ";" + imsProxyProtocol);
        }
    }

    private Record[] getDnsNAPTR(String domain) {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("DNS NAPTR lookup for " + domain);
            }
            Lookup lookup = new Lookup(domain, Type.NAPTR);
            lookup.setCache(null);
            Record[] result = lookup.run();
            int code = lookup.getResult();
            if (code != Lookup.SUCCESSFUL) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Lookup error: " + code + "/" + lookup.getErrorString());
                }
            }
            return result;
        } catch (TextParseException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Not a valid DNS name");
            }
            return null;
        } catch (IllegalArgumentException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Not a valid DNS type");
            }
            return null;
        }
    }

    private Record[] getDnsSRV(String domain) {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("DNS SRV lookup for " + domain);
            }
            Lookup lookup = new Lookup(domain, Type.SRV);
            lookup.setCache(null);
            Record[] result = lookup.run();
            int code = lookup.getResult();
            if (code != Lookup.SUCCESSFUL) {
                if (sLogger.isActivated()) {
                    sLogger.warn("Lookup error: " + code + "/" + lookup.getErrorString());
                }
            }
            return result;
        } catch (TextParseException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Not a valid DNS name");
            }
            return null;
        } catch (IllegalArgumentException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Not a valid DNS type");
            }
            return null;
        }
    }

    private String getDnsA(String domain) {
        try {
            if (sLogger.isActivated()) {
                sLogger.debug("DNS A lookup for " + domain);
            }
            return InetAddress.getByName(domain).getHostAddress();
        } catch (UnknownHostException e) {
            if (sLogger.isActivated()) {
                sLogger.debug("Unknown host for " + domain);
            }
            return null;
        }
    }

    private SRVRecord getBestDnsSRV(Record[] records) {
        SRVRecord result = null;
        for (Record record : records) {
            SRVRecord srv = (SRVRecord) record;
            if (sLogger.isActivated()) {
                sLogger.debug("SRV record: " + srv.toString());
            }
            if (result == null) {
                // First record
                result = srv;
            } else {
                // Next record
                if (srv.getPriority() < result.getPriority()) {
                    // Lowest priority
                    result = srv;
                } else if (srv.getPriority() == result.getPriority()) {
                    // Highest weight
                    if (srv.getWeight() > result.getWeight()) {
                        result = srv;
                    }
                }
            }
        }
        return result;
    }
}
