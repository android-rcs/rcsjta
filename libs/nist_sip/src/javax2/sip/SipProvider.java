package javax2.sip;

import java.util.TooManyListenersException;

import javax2.sip.header.CallIdHeader;
import javax2.sip.message.Request;
import javax2.sip.message.Response;

public interface SipProvider {
    /**
     * @deprecated
     * @see #addListeningPoint(ListeningPoint)
     */
    void setListeningPoint(ListeningPoint listeningPoint)
            throws ObjectInUseException;
    void addListeningPoint(ListeningPoint listeningPoint)
            throws ObjectInUseException;
    void removeListeningPoint(ListeningPoint listeningPoint)
            throws ObjectInUseException;
    void removeListeningPoints();

    /**
     * @deprecated
     * @see #getListeningPoints()
     */
    ListeningPoint getListeningPoint();
    ListeningPoint getListeningPoint(String transport);
    ListeningPoint[] getListeningPoints();

    void addSipListener(SipListener sipListener)
            throws TooManyListenersException;
    void removeSipListener(SipListener sipListener);

    CallIdHeader getNewCallId();

    ClientTransaction getNewClientTransaction(Request request)
            throws TransactionUnavailableException;
    ServerTransaction getNewServerTransaction(Request request)
            throws TransactionAlreadyExistsException,
            TransactionUnavailableException;

    Dialog getNewDialog(Transaction transaction) throws SipException;

    boolean isAutomaticDialogSupportEnabled();
    void setAutomaticDialogSupportEnabled(boolean flag);

    SipStack getSipStack();

    void sendRequest(Request request) throws SipException;
    void sendResponse(Response response) throws SipException;
}

