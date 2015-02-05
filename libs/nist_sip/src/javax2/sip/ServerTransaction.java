package javax2.sip;

import javax2.sip.message.Response;

public interface ServerTransaction extends Transaction {
    void sendResponse(Response response)
            throws SipException, InvalidArgumentException;

    void enableRetransmissionAlerts() throws SipException;

    ServerTransaction getCanceledInviteTransaction();
}
