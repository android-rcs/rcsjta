
package com.orangelabs.rcs.utils;

import com.orangelabs.rcs.utils.IpAddressUtils;

import android.test.AndroidTestCase;

/**
 * @author jexa7410
 *
 */
public class IpAddressUtilsTest extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testExtractHostAddress() {
		assertEquals(IpAddressUtils.extractHostAddress("127.0.0.1"), "127.0.0.1");	
		assertEquals(IpAddressUtils.extractHostAddress("domain.com"), "domain.com");	
		assertEquals(IpAddressUtils.extractHostAddress("127.0.0.1%2"), "127.0.0.1");
		
		assertEquals(IpAddressUtils.extractHostAddress("fe80::1234%1"), "fe80::1234");
		assertEquals(IpAddressUtils.extractHostAddress("ff02::5678%pvc1.3"), "ff02::5678");
		assertEquals(IpAddressUtils.extractHostAddress("169.254.0.0/16"), "169.254.0.0");
	}
}
