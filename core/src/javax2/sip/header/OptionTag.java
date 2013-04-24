package javax2.sip.header;

import java.text.ParseException;

public interface OptionTag {
    String getOptionTag();
    void setOptionTag(String optionTag) throws ParseException;
}
