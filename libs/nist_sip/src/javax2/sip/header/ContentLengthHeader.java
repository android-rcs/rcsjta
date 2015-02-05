package javax2.sip.header;

import javax2.sip.InvalidArgumentException;

public interface ContentLengthHeader extends Header {
    String NAME = "Content-Length";

    int getContentLength();
    void setContentLength(int contentLength) throws InvalidArgumentException;
}
