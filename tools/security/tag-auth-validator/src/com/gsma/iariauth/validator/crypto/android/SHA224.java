package com.gsma.iariauth.validator.crypto.android;

import java.security.Provider;

import com.android.org.bouncycastle.jcajce.provider.digest.BCMessageDigest;

public class SHA224 extends Provider {
	private static final long serialVersionUID = -2531496314262562364L;

	public SHA224() {
		super("SHA224", 1.0, "SHA224 Provider");
		put("MessageDigest.SHA-224", "com.gsma.iariauth.validator.crypto.android.SHA224$Digest");
	}

    static public class Digest extends BCMessageDigest implements Cloneable {
        public Digest() {
            super(new SHA224Digest());
        }

        public Object clone() throws CloneNotSupportedException {
            Digest d = (Digest)super.clone();
            d.digest = new SHA224Digest((SHA224Digest)digest);

            return d;
        }
    }
}
