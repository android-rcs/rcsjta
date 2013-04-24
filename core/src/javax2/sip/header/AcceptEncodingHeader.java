package javax2.sip.header;

import javax2.sip.InvalidArgumentException;

public interface AcceptEncodingHeader extends Encoding, Header, Parameters {
    String NAME = "Accept-Encoding";

    float getQValue();
    void setQValue(float qValue) throws InvalidArgumentException;
}
