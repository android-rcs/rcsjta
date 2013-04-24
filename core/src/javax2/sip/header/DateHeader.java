package javax2.sip.header;

import java.util.Calendar;

public interface DateHeader extends Header {
    String NAME = "Date";

    Calendar getDate();
    void setDate(Calendar date);
}
