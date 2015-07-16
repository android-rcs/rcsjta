/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 ******************************************************************************/

package com.gsma.iariauth.sample;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.security.MessageDigest;

public class FingerprintUtil {

    public static String getFingerprint(Context context) {

        try {
            Signature[] sigs = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_SIGNATURES).signatures;
            if (sigs.length < 1) {
                return null;
            }

            // take only the first
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(sigs[0].toByteArray());
            byte[] digest = md.digest();

            String toRet = "";
            for (int i = 0; i < digest.length; i++) {
                if (i != 0)
                    toRet = toRet.concat(":");
                int b = digest[i] & 0xff;
                String hex = Integer.toHexString(b);
                if (hex.length() == 1)
                    toRet = toRet.concat("0");
                toRet = toRet.concat(hex);
            }
            return toRet.toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

}
