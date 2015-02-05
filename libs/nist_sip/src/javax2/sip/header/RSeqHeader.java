package javax2.sip.header;

import javax2.sip.InvalidArgumentException;

public interface RSeqHeader extends Header {
    String NAME = "RSeq";

    long getSeqNumber();
    void setSeqNumber(long sequenceNumber) throws InvalidArgumentException;

    /**
     * @deprecated
     * @see #getSeqNumber()
     */
    int getSequenceNumber();

    /**
     * @deprecated
     * @see #setSeqNumber(long)
     */
    void setSequenceNumber(int sequenceNumber) throws InvalidArgumentException;
}
