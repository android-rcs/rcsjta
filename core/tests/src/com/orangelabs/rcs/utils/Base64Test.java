package com.orangelabs.rcs.utils;

import android.test.AndroidTestCase;
import com.orangelabs.rcs.utils.Base64;

public class Base64Test extends AndroidTestCase {
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public final void testBase64() {
		String ss = "2 + 2 = quatre, non 5?";
		assertEquals(Base64.encodeBase64ToString(ss.getBytes()), "MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==");
		assertEquals(new String(Base64.encodeBase64(ss.getBytes())), "MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==");
		assertEquals(Base64.encodeBase64ToString(Base64.decodeBase64((new String("MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==")).getBytes())), "MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==");
		assertEquals(new String(Base64.decodeBase64((new String("MiArIDIgPSBxdWF0cmUsIG5vbiA1Pw==")).getBytes())), ss);
	}

	
}
