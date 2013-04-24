package javax2.sip.header;

import javax2.sip.InvalidArgumentException;

public interface MaxForwardsHeader extends Header {
    String NAME = "Max-Forwards";

    void decrementMaxForwards() throws TooManyHopsException;

    int getMaxForwards();
    void setMaxForwards(int maxForwards) throws InvalidArgumentException;

    boolean hasReachedZero();
}
