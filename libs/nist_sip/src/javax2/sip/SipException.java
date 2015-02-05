package javax2.sip;

public class SipException extends Exception {
    public SipException() {
    }

    public SipException(String message) {
        super(message);
    }

    public SipException(String message, Throwable cause) {
        super(message, cause);
    }
}

