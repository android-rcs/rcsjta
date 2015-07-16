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

package com.gsma.rcs.security;

import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.security.AuthorizationData;
import com.gsma.rcs.provider.security.AuthorizationData.Type;
import com.gsma.rcs.provider.security.CacheAuth;
import com.gsma.rcs.provider.security.RevocationData;
import com.gsma.rcs.provider.security.SecurityLog;

import android.test.AndroidTestCase;

import java.lang.reflect.Field;
import java.util.Map;

public class SecurityProviderTest extends AndroidTestCase {

    private final static int REV_AUTHORIZED = -1;
    private final static int REV_REVOKED_INFINITE = 0;

    private String iari1 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo1";
    private String iari2 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc000.mcc000.demo2";

    private final Integer UID1 = 100000;
    private final Integer UID2 = 100001;
    private final Integer UID3 = 100002;

    private SecurityLog mSecurityInfos;

    private LocalContentResolver mContentResolver;

    private AuthorizationData mAuth1;
    private AuthorizationData mAuth2;
    private AuthorizationData mAuth3;
    private AuthorizationData mAuth4;

    private RevocationData mRev1;
    private RevocationData mRev2;

    private SecurityLibTest mSecurityInfosTest;

    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = new LocalContentResolver(getContext().getContentResolver());
        mSecurityInfos = SecurityLog.getInstance(mContentResolver);
        mAuth1 = new AuthorizationData(UID1, "com.orangelabs.package1", iari1, Type.APPLICATION_ID);
        mAuth2 = new AuthorizationData(UID2, "com.orangelabs.package2", iari2, Type.SERVICE_ID);
        mAuth3 = new AuthorizationData(UID3, "com.orangelabs.package3", "demo3", Type.SERVICE_ID);
        mAuth4 = new AuthorizationData(UID3, "com.orangelabs.package3", "demo4", Type.SERVICE_ID);
        mSecurityInfosTest = new SecurityLibTest();
        mSecurityInfosTest.removeAllAuthorizations(mContentResolver);

        mRev1 = new RevocationData(iari1, REV_AUTHORIZED);
        mRev2 = new RevocationData(iari2, REV_REVOKED_INFINITE);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetAllAuthorizations() {
        Map<AuthorizationData, Integer> authorizationDatas = mSecurityInfos.getAllAuthorizations();
        assertEquals(0, authorizationDatas.size());

        mSecurityInfos.addAuthorization(mAuth1);
        authorizationDatas = mSecurityInfos.getAllAuthorizations();
        assertEquals(1, authorizationDatas.size());
        assertTrue(authorizationDatas.containsKey(mAuth1));

        mSecurityInfos.addAuthorization(mAuth2);
        authorizationDatas = mSecurityInfos.getAllAuthorizations();
        assertEquals(2, authorizationDatas.size());
        assertTrue(authorizationDatas.containsKey(mAuth1));
        assertTrue(authorizationDatas.containsKey(mAuth2));

        mSecurityInfos.addAuthorization(mAuth3);
        authorizationDatas = mSecurityInfos.getAllAuthorizations();
        assertEquals(3, authorizationDatas.size());
        assertTrue(authorizationDatas.containsKey(mAuth1));
        assertTrue(authorizationDatas.containsKey(mAuth2));
        assertTrue(authorizationDatas.containsKey(mAuth3));

        mSecurityInfos.addAuthorization(mAuth4);
        authorizationDatas = mSecurityInfos.getAllAuthorizations();
        assertEquals(4, authorizationDatas.size());
        assertTrue(authorizationDatas.containsKey(mAuth1));
        assertTrue(authorizationDatas.containsKey(mAuth2));
        assertTrue(authorizationDatas.containsKey(mAuth3));
        assertTrue(authorizationDatas.containsKey(mAuth4));
    }

    public void testAddAuthorization() {
        mSecurityInfos.addAuthorization(mAuth1);
        Integer id = mSecurityInfosTest.getIdForPackageUidAndIari(mContentResolver, UID1,
                "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo1");
        assertNotSame(id, SecurityLibTest.INVALID_ID);

        Map<AuthorizationData, Integer> authorizationDatas = mSecurityInfos.getAllAuthorizations();
        assertEquals(1, authorizationDatas.size());

        assertTrue(authorizationDatas.containsKey(mAuth1));

        assertEquals(id, authorizationDatas.get(mAuth1));

        mSecurityInfos.addAuthorization(mAuth1);
        Integer new_id = mSecurityInfosTest.getIdForPackageUidAndIari(mContentResolver, UID1,
                "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo1");
        assertEquals(id, new_id);

        authorizationDatas = mSecurityInfos.getAllAuthorizations();
        assertEquals(1, authorizationDatas.size());
        assertTrue(authorizationDatas.containsKey(mAuth1));
        assertEquals(authorizationDatas.get(mAuth1), id);
    }

    public void testRemoveAuthorization() {
        mSecurityInfos.addAuthorization(mAuth1);
        int id = mSecurityInfosTest.getIdForPackageUidAndIari(mContentResolver, UID1,
                "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo1");
        assertNotSame(id, SecurityLibTest.INVALID_ID);
        int count = mSecurityInfos.removeAuthorization(id, mAuth1);
        assertEquals(1, count);
        Map<AuthorizationData, Integer> map = mSecurityInfos.getAllAuthorizations();
        assertEquals(0, map.size());
    }

    public void testGetAuthorization() {
        Integer id1, id2;
        AuthorizationData localAuth;

        Map<AuthorizationData, Integer> authorizationDatas = mSecurityInfos.getAllAuthorizations();
        assertEquals(0, authorizationDatas.size());

        mSecurityInfos.addAuthorization(mAuth1);
        authorizationDatas = mSecurityInfos.getAllAuthorizations();
        assertEquals(1, authorizationDatas.size());
        assertTrue(authorizationDatas.containsKey(mAuth1));

        id1 = mSecurityInfosTest
                .getIdForPackageUidAndIari(mContentResolver, UID1, mAuth1.getIari());
        id2 = mSecurityInfosTest
                .getIdForPackageUidAndIari(mContentResolver, UID1, mAuth1.getIari());
        assertEquals(id1, id2);

        localAuth = mSecurityInfosTest.getAuthorizationById(mContentResolver, id1);
        assertEquals(mAuth1, localAuth);

        localAuth = mSecurityInfos.getServiceAuthorization(mAuth1.getIari());
        assertEquals(mAuth1, localAuth);

        assertEquals(id1,
                mSecurityInfos.getAuthorizationIdByIariAndType(mAuth1.getIari(), mAuth1.getType()));
        assertEquals(Integer.valueOf(SecurityLog.INVALID_ID),
                mSecurityInfos.getAuthorizationIdByIariAndType("notExistingIARI", Type.SERVICE_ID));
    }

    public void testRevocations() {
        Map<RevocationData, Integer> revocationDatas = mSecurityInfosTest
                .getAllRevocations(mContentResolver);
        assertEquals(0, revocationDatas.size());
        assertEquals(0, getCacheForRevocationAuth(mSecurityInfos).size());

        // Add
        mSecurityInfos.addRevocation(mRev1);
        revocationDatas = mSecurityInfosTest.getAllRevocations(mContentResolver);
        int revId1 = mSecurityInfos.getIdForRevocation(mRev1.getServiceId());
        assertNotSame(revId1, SecurityLibTest.INVALID_ID);
        assertEquals(1, revocationDatas.size());
        assertEquals(1, getCacheForRevocationAuth(mSecurityInfos).size());
        assertTrue(revocationDatas.containsKey(mRev1));

        mSecurityInfos.addRevocation(mRev2);
        int revId2 = mSecurityInfos.getIdForRevocation(mRev2.getServiceId());
        assertNotSame(revId1, SecurityLibTest.INVALID_ID);
        revocationDatas = mSecurityInfosTest.getAllRevocations(mContentResolver);
        assertEquals(2, revocationDatas.size());
        assertEquals(2, getCacheForRevocationAuth(mSecurityInfos).size());
        assertTrue(revocationDatas.containsKey(mRev1));
        assertTrue(revocationDatas.containsKey(mRev2));

        // Update
        RevocationData localRev1 = new RevocationData(mRev1.getServiceId(), 1000L);
        RevocationData localRev2 = new RevocationData(mRev2.getServiceId(), 2000L);
        mSecurityInfos.addRevocation(localRev1);
        mSecurityInfos.addRevocation(localRev1);
        revocationDatas = mSecurityInfosTest.getAllRevocations(mContentResolver);
        assertEquals(2, revocationDatas.size());
        assertEquals(2, getCacheForRevocationAuth(mSecurityInfos).size());
        assertEquals(revId1, mSecurityInfos.getIdForRevocation(localRev1.getServiceId()));
        assertEquals(revId2, mSecurityInfos.getIdForRevocation(localRev2.getServiceId()));

        // Remove
        mSecurityInfos.removeRevocation(localRev1.getServiceId());
        revocationDatas = mSecurityInfosTest.getAllRevocations(mContentResolver);
        assertEquals(1, revocationDatas.size());
        assertEquals(1, getCacheForRevocationAuth(mSecurityInfos).size());
        mSecurityInfos.removeRevocation(localRev2.getServiceId());
        revocationDatas = mSecurityInfosTest.getAllRevocations(mContentResolver);
        assertEquals(0, revocationDatas.size());
        assertEquals(0, getCacheForRevocationAuth(mSecurityInfos).size());
    }

    public void testCacheAuthorization() {

        CacheAuth cacheAuth = new CacheAuth();
        cacheAuth.add(mAuth1);
        assertEquals(mAuth1, cacheAuth.getServiceAuth(iari1));
        assertEquals(mAuth1, cacheAuth.getApplicationAuth(UID1));

        cacheAuth.remove(mAuth1);
        assertNull(cacheAuth.getServiceAuth(iari1));
        assertNull(cacheAuth.getApplicationAuth(UID1));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getCacheForRevocationAuth(SecurityLog securityLog) {
        Field cacheField;
        try {
            cacheField = SecurityLog.class.getDeclaredField("mCacheRev");
            cacheField.setAccessible(true);
            return (Map<String, Integer>) cacheField.get(securityLog);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
