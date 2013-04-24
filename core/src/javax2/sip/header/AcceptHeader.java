package javax2.sip.header;

import javax2.sip.InvalidArgumentException;

public interface AcceptHeader extends Header, MediaType, Parameters {
    String NAME = "Accept";

    boolean allowsAllContentSubTypes();
    boolean allowsAllContentTypes();

    float getQValue();
    void setQValue(float qValue) throws InvalidArgumentException;
    boolean hasQValue();
    void removeQValue();
}
