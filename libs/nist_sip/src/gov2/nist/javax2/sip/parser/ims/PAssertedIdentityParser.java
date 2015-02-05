/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), an agency of the Federal Government.
* Pursuant to title 15 Untied States Code Section 105, works of NIST
* employees are not subject to copyright protection in the United States
* and are considered to be in the public domain.  As a result, a formal
* license is not needed to use the software.
*
* This software is provided by NIST as a service and is expressly
* provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
* Permission to use this software is contingent upon your acceptance
* of the terms of this agreement
*
* .
*
*/
/*******************************************
 * PRODUCT OF PT INOVACAO - EST DEPARTMENT *
 *******************************************/

package gov2.nist.javax2.sip.parser.ims;


import java.text.ParseException;


import gov2.nist.javax2.sip.header.SIPHeader;
import gov2.nist.javax2.sip.header.ims.PAssertedIdentity;
import gov2.nist.javax2.sip.header.ims.PAssertedIdentityList;
import gov2.nist.javax2.sip.header.ims.SIPHeaderNamesIms;
import gov2.nist.javax2.sip.parser.AddressParametersParser;
import gov2.nist.javax2.sip.parser.Lexer;
import gov2.nist.javax2.sip.parser.TokenTypes;

/**
 * @author ALEXANDRE MIGUEL SILVA SANTOS
 */

public class PAssertedIdentityParser
    extends AddressParametersParser
    implements TokenTypes{

    /**
     * Constructor
     * @param assertedIdentity -  message to parse to set
     */
    public PAssertedIdentityParser(String assertedIdentity) {
        super(assertedIdentity);

    }

    protected PAssertedIdentityParser(Lexer lexer) {
        super(lexer);

    }


    public SIPHeader parse() throws ParseException {

        if (debug)
            dbg_enter("AssertedIdentityParser.parse");

        PAssertedIdentityList assertedIdList = new PAssertedIdentityList();

        try {

            headerName(TokenTypes.P_ASSERTED_IDENTITY);

            PAssertedIdentity pai = new PAssertedIdentity();
            pai.setHeaderName(SIPHeaderNamesIms.P_ASSERTED_IDENTITY);

            super.parse(pai);
            assertedIdList.add(pai);

            this.lexer.SPorHT();
            while (lexer.lookAhead(0) == ',')
            {
                this.lexer.match(',');
                this.lexer.SPorHT();

                pai = new PAssertedIdentity();
                super.parse(pai);
                assertedIdList.add(pai);

                this.lexer.SPorHT();
            }
            this.lexer.SPorHT();
            this.lexer.match('\n');

            return assertedIdList;

        }

        finally {
            if (debug)
                dbg_leave("AssertedIdentityParser.parse");
            }
    }
}
