package javax2.sip;

import gov2.nist.javax2.sip.ListeningPointImpl;
import gov2.nist.javax2.sip.SipProviderImpl;

import java.util.Collection;
import java.util.Iterator;

import javax2.sip.address.Router;

public interface SipStack {
    /**
     * Deprecated. Use {@link #createListeningPoint(String, int, String)}
     * instead.
     */
    ListeningPoint createListeningPoint(int port, String transport)
            throws TransportNotSupportedException, InvalidArgumentException;
    ListeningPoint createListeningPoint(String ipAddress, int port,
            String transport) throws TransportNotSupportedException,
            InvalidArgumentException;
    void deleteListeningPoint(ListeningPoint listeningPoint)
            throws ObjectInUseException;

    SipProvider createSipProvider(ListeningPoint listeningPoint)
            throws ObjectInUseException;
    void deleteSipProvider(SipProvider sipProvider) throws ObjectInUseException;

    Collection<Dialog> getDialogs();
    String getIPAddress();
    Iterator<ListeningPointImpl> getListeningPoints();
    Router getRouter();
    Iterator<SipProviderImpl> getSipProviders();
    String getStackName();
    
    // Changed by Deutsche Telekom
     int getMtuSize();
     
    /**
     * @deprecated
     * Use {@link ServerTransaction#enableRetransmissionAlerts()} to enable
     * retransmission alerts instead.
     */
    boolean isRetransmissionFilterActive();

    void start() throws ProviderDoesNotExistException, SipException;
    void stop();
}

