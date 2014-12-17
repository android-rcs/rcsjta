/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package android.tests.provider;

import android.test.InstrumentationTestCase;

public class Utils extends InstrumentationTestCase  {
	
	public static void checkProjection(String[] expected, String[] obtained) {
		if (expected != null) {
			if (obtained == null) {
				fail("projection is null");
			}
			assertEquals(expected.length, obtained.length);
			for (String field1 : expected) {
				boolean found = false;
				for (String field2 : obtained) {
					if (field1.equals(field2)) {
						found = true;
						break;
					}
				}
				if (!found) {
					fail("field1 is not present");
				}
			}
		} else {
			if (obtained != null) {
				fail("invalid Projection");
			}
		}
	}
}
