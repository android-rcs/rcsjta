package javax2.sip.address;

import java.util.ListIterator;

import javax2.sip.SipException;
import javax2.sip.message.Request;

public interface Router {
    Hop getNextHop(Request request) throws SipException;
    ListIterator getNextHops(Request request);
    Hop getOutboundProxy();
}

