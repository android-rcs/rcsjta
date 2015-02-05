package javax2.sip.header;

import javax2.sip.InvalidArgumentException;

public interface MimeVersionHeader extends Header {
    String NAME = "MIME-Version";

    int getMajorVersion();
    void setMajorVersion(int majorVersion) throws InvalidArgumentException;

    int getMinorVersion();
    void setMinorVersion(int minorVersion) throws InvalidArgumentException;
}
