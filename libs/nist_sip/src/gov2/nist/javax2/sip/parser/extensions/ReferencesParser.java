package gov2.nist.javax2.sip.parser.extensions;

import gov2.nist.core.Token;
import gov2.nist.javax2.sip.header.Reason;
import gov2.nist.javax2.sip.header.ReasonList;
import gov2.nist.javax2.sip.header.SIPHeader;
import gov2.nist.javax2.sip.header.extensions.References;
import gov2.nist.javax2.sip.parser.Lexer;
import gov2.nist.javax2.sip.parser.ParametersParser;
import gov2.nist.javax2.sip.parser.TokenTypes;

import java.text.ParseException;

public class ReferencesParser extends ParametersParser {
    /**
     * Creates a new instance of ReferencesParser
     * @param references the header to parse
     */
    public ReferencesParser(String references) {
        super(references);
    }

    /**
     * Constructor
     * @param lexer the lexer to use to parse the header
     */
    protected ReferencesParser(Lexer lexer) {
        super(lexer);
    }
    
    /**
     * parse the String message
     * @return SIPHeader (ReasonParserList object)
     * @throws SIPParseException if the message does not respect the spec.
     */
    public SIPHeader parse() throws ParseException {
       
        if (debug)
            dbg_enter("ReasonParser.parse");

        try {
            headerName(TokenTypes.REFERENCES);
            References references= new References();
            this.lexer.SPorHT();
               
            String callId = lexer.byteStringNoSemicolon();
           
            references.setCallId(callId);
            super.parse(references);
            return references;
       } finally {
            if (debug)
                dbg_leave("ReferencesParser.parse");
        }

      
    }

}
