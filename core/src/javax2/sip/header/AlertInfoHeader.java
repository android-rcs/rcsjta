package javax2.sip.header;

import javax2.sip.address.URI;

public interface AlertInfoHeader extends Header, Parameters {
    String NAME = "Alert-Info";

    URI getAlertInfo();
    void setAlertInfo(URI alertInfo);
    void setAlertInfo(String alertInfo);
}
