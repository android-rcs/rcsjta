package gov2.nist.javax2.sip.message;

import javax2.sip.header.ContentDispositionHeader;
import javax2.sip.header.ContentTypeHeader;

public interface Content {

    public abstract void setContent(Object content);
    
    public abstract Object getContent();
 
    public abstract ContentTypeHeader getContentTypeHeader();
    
    public abstract ContentDispositionHeader getContentDispositionHeader();

    /**
     * The default packing method. This packs the content to be appended to the
     * sip message.
     * 
     */
    public abstract String toString();

 
}
