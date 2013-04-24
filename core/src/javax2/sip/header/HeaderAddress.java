package javax2.sip.header;

import javax2.sip.address.Address;

public interface HeaderAddress {
    Address getAddress();
    void setAddress(Address address);
}
