package gov2.nist.javax2.sip.stack;

import gov2.nist.javax2.sip.message.SIPMessage;

public interface RawMessageChannel {

    public abstract void processMessage(SIPMessage sipMessage) throws Exception ;

}
