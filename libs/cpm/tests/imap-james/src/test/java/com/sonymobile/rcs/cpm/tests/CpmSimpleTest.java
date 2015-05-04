
package com.sonymobile.rcs.cpm.tests;

import org.junit.Test;

import java.util.logging.Logger;

public class CpmSimpleTest {

    Logger log = Logger.getLogger("com.sonymobile.rcs");

    @Test
    public void testMe() {

        System.out.println("hello cpm tests");
        log.fine("fine log");
        log.info("info log");
        log.severe("error log");
    }

}
