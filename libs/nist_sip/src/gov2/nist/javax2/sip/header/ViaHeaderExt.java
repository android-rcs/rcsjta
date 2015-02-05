/*
 * This code has been contributed by the authors to the public domain.
 */
package gov2.nist.javax2.sip.header;

import javax2.sip.header.ViaHeader;


/**
 * @author jean.deruelle@gmail.com
 *
 */
public interface ViaHeaderExt extends ViaHeader {
    /**
     * Returns hostname:port as a string equivalent to the "sent-by" field
     * @return "sent-by" field
     * @since 2.0
     */
    public String getSentByField();
    /**
     * Returns transport to the "sent-protocol" field
     * @return "sent-protocol" field
     * @since 2.0
     */
    public String getSentProtocolField();
}
