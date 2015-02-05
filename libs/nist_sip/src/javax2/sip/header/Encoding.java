package javax2.sip.header;

import java.text.ParseException;

public interface Encoding {
    String getEncoding();
    void setEncoding(String encoding) throws ParseException;
}
