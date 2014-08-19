package com.orangelabs.rcs.utils;

import java.util.Date;

import android.test.AndroidTestCase;

import com.orangelabs.rcs.utils.DateUtils;

public class DateUtilsTest extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testEncodeDecode() {
		long t = System.currentTimeMillis();
		String encoded = DateUtils.encodeDate(t);
		long decoded = DateUtils.decodeDate(encoded);
	
		assertEquals(new Date(t).getYear(), new Date(decoded).getYear());
		assertEquals(new Date(t).getMonth(), new Date(decoded).getMonth());
		assertEquals(new Date(t).getDay(), new Date(decoded).getDay());
		assertEquals(new Date(t).getHours(), new Date(decoded).getHours());
		assertEquals(new Date(t).getMinutes(), new Date(decoded).getMinutes());
		assertEquals(new Date(t).getSeconds(), new Date(decoded).getSeconds());
		assertEquals(new Date(t).getTimezoneOffset(), new Date(decoded).getTimezoneOffset());
	}
}
