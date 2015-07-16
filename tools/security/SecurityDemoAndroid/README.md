# SecurityDemoAndroid

SecurityDemoAndroid is a trivial Android app that loads the IARIValidator and processes a IARI Authorization document.

You will need to copy the IARI documents to the assets directory of the Android project:

    cd rcsjta/tools/security/
	cp test/tag-auth-util/test/<iari-authorization-document>.xml SecurityDemoAndroid/assets/iari_authorization.<n>.xml 

	With:
	<iari-authorization-document>.xml : the IARI auth document defining an extension having format "ext.ss.<extension-id>".
	iari_authorization.<n>.xml : the renamed IARI auth document for the nth Extension ID.
	
	
The AndroidManifest.xml must contain a metadata as following:
	
	<meta-data
            android:name="com.gsma.services.rcs.capability.EXTENSION.<n>"
            android:value="<extension-id>" />
	
	With:
	com.gsma.services.rcs.capability.EXTENSION.<n> the key for nth Extension ID.
	<extension-id> : the value set to the extension-specific part of the IARI string.

The SecurityDemoAndroid must be signed with the rcsjta/tools/security/test/tag-auth-util/test/keys/package-signer.jks

Install the application using ADB tool:

	> adb install SecurityDemoAndroid.apk