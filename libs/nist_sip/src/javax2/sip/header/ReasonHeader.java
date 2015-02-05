package javax2.sip.header;

import java.text.ParseException;

import javax2.sip.InvalidArgumentException;

public interface ReasonHeader extends Header, Parameters {
    String NAME = "Reason";

    int getCause();
    void setCause(int cause) throws InvalidArgumentException;

    String getProtocol();
    void setProtocol(String protocol) throws ParseException;

    String getText();
    void setText(String text) throws ParseException;
}
