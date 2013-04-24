package javax2.sip.address;

import java.text.ParseException;
import java.util.Iterator;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.Parameters;

public interface SipURI extends URI, Parameters {
    boolean isSecure();
    void setSecure(boolean secure);

    String getHeader(String name);
    void setHeader(String name, String value);
    Iterator getHeaderNames();

    String getHost();
    void setHost(String host) throws ParseException;

    String getLrParam();
    void setLrParam();
    boolean hasLrParam();

    String getMAddrParam();
    void setMAddrParam(String mAddrParam) throws ParseException;

    int getPort();
    void setPort(int port) throws InvalidArgumentException;

    int getTTLParam();
    void setTTLParam(int ttlParam);

    String getTransportParam();
    void setTransportParam(String transportParam) throws ParseException;
    boolean hasTransport();

    String getUser();
    void setUser(String user);
    String getUserParam();
    void setUserParam(String userParam);

    String getUserType();
    void removeUserType();

    String getUserPassword();
    void setUserPassword(String userPassword);

    String getUserAtHost();
    String getUserAtHostPort();

    String getMethodParam();
    void setMethodParam(String methodParam) throws ParseException;
}

