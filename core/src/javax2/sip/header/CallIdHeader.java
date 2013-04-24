package javax2.sip.header;

import java.text.ParseException;

public interface CallIdHeader extends Header {
    String NAME = "Call-ID";

    String getCallId();
    void setCallId(String callId) throws ParseException;
}
