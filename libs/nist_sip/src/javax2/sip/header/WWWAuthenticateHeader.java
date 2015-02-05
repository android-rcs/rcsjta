package javax2.sip.header;

import javax2.sip.address.URI;

public interface WWWAuthenticateHeader extends AuthorizationHeader {
    String NAME = "WWW-Authenticate";

    /**
     * @deprecated This method should return null.
     */
    URI getURI();

    /**
     * @deprecated This method should return immediately.
     */
    void setURI(URI uri);
}
