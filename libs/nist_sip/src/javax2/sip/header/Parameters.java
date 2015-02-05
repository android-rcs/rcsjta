package javax2.sip.header;

import gov2.nist.core.NameValue;

import java.text.ParseException;
import java.util.Iterator;

public interface Parameters {
    String getParameter(String name);
    void setParameter(String name, String value) throws ParseException;
    void setParameter(NameValue nameValue) throws ParseException;

    Iterator getParameterNames();
    void removeParameter(String name);
}
