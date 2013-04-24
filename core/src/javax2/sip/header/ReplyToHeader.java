package javax2.sip.header;

public interface ReplyToHeader extends HeaderAddress, Header, Parameters {
    String NAME = "Reply-To";

    String getDisplayName();
}
