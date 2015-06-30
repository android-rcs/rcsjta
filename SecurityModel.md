


---

# Security #
## Principle ##
All Android applications must be signed. Application or code signing is the process of digitally signing a given application using a private key to:
  * Identify the code's author
  * Detect if the application has changed
  * Establish trust between applications

**At dev time**
For each RCS capability (IARI tag) :
  * The dev generates the unique tag (IARI) and its tag certificate (signature + public key).
  * The dev generates a signed Tag Authorization (XML file) to associate the tag and the app.
  * This authorization is then packaged as part of the app.

**At install time**
The system checks the application (verifying package's signature).
Using the same certificate, the RCS stack checks if the app is authorized to use the tag/capability.

Then the system updates the provider data, adding/removing extensions, **from the provisioning source**. The provider data plays the role of a local cache for authorized extensions for the user/terminal pair.
The system checks each capability of the application for this user on this terminal.
Then the user is warned if some capabilities are restricted for him on his terminal.

**At run time**
The system checks each capability of the application for **this user** on **this terminal**.
Then the user is warned if some capabilities are restricted for him on his terminal.
It tests the incoming application against the provisioning data to know if the application can or cannot use RCS services regarding used extensions.
The provider data is updated to take into account the extensions removed or added to the provisioning data
The glue provides to the validator the necessary data in order allowing it to validate or not.


---


## Tools ##
Useful tools for security objects creation, inspection and manipulation.
  * <a href='http://portecle.sourceforge.net/'>PorteCle</a>  is an open source application for creating, managing and examining keystores, keys, certificates, certificate requests, certificate revocation lists...
  * keytool is an Android JDK tool
  * iaritool: RCS tool (source code is under `rcsjta/tools/security/tag-auth-util` sub-project)

## Build the validator library ##
  1. Open the `RCSJTA_tools/tag-auth-validator` project, the source code is at
```
.../git/rcsjta/tools/security/tag-auth-validator
```
  1. Run the ant default task (jar)

## Build the iaritool application ##
Requirement: the validator library
  1. Open the `RCSJTA_tools/tag-auth-util` project, the source code is at
```
.../git/rcsjta/tools/security/tag-auth-util
```
  1. Run the ant default task (_iarivalidator_)

## Usage of security keys ##
Use or generate keys in `RCSJTA_tools/tag-auth-util/test/keys ` by running shell batch commands.

Keys relevant to security tests are:

**keys/com.gsma.iariauth.sample.jks**

This is the example IARI key and certificate for the IARI:
`urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.iari.range.test`

**keys/range-root-truststore.jks**

This is the trust root for the range:
`urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.*`

**keys/range-root-truststore.bks**

This is the equivalent to the above, but in `.bks` format (BouncyCastle encryption, as in Android).

**keys/package-signer.jks**

This is the example key that the developer uses to sign his package/app.

You can inspect this with the keytool utility;
if you run this command :
```
keytool -list -keystore keys/package-signer.jks
```

the result must b like:
```
Keystore type: JKS
Keystore provider: SUN

Your keystore contains 1 entry

package-signer, 07-Jun-2014, PrivateKeyEntry, 
Certificate fingerprint (SHA1): 7B:47:29:1F:30:46:B5:D9:6C:D9:53:72:23:B0:6E:8F:09:40:E0:A8
```

**mnc099.mcc099.messaging-range-ext0**,**mnc099.mcc099.messaging-range-ext1** and **ext.2eaElQ4box3WNV\_7MWwoxtYfnkDQiMq8wnR7vw.xml**

For the purposes of the test, this is the xml document that would normally be extracted from the application package.

**range-root-truststore.bks**

This is a keystore containing the trust root for the IARI range we are checking for.

## Generate the security keys and IARI document for a MNO application (range mode) ##
**WARNING**: You must achieve this procedure for one extension before generating keys and IARI document for a second extension.

**WARNING**: generating new keys implies to copy some generated files to many places in order to be sure
  1. Go to the directory `/git/rcsjta/tools/security/tag-auth-util/test/keys`
  1. Only files beginning with "`_`" (underscore) are necessary, the others can be generated
  1. The `_create-range_root_keys.sh` is intended to create keys for range owner. BE CAREFUL: running this batch will erase current value for these keys, so all IARI documents are to be regenerated as well as validated application .
  1. The `_create-range_keys_extension0.sh` (resp. `_create-range_keys_extension1.sh`) generate keys and certificates for extension `mnc099.mcc099.messaging-range-ext0` (resp. `mnc099.mcc099.messaging-range-ext1`)
  1. So run `_create-range_root_keys.sh` if you want to generate keys from scratch, then
  1. Run `_create-range_keys_extension0.sh` (resp. `_create-range_keys_extension1.sh`) to generate the according IARI document
  1. Go to the directory `/git/rcsjta/tools/security/tag-auth-util/test`
  1. Edit the file `tag-auth-util/test/range-sign-extension0.sh` (resp. `tag-auth-util/test/range-sign-extension1.sh`) and verify the declared application package name (`com.gsma.iariauth.sample` in this case).
  1. Run `tag-auth-util/test/range-sign-extension0.sh` (resp. `tag-auth-util/test/range-sign-extension1.sh`)
  1. A new IARI document should be generated in this directory, with the name `mnc099.mcc099.messaging-range-ext0.xml` (resp. `mnc099.mcc099.messaging-range-ext1.xml`).
  1. This extension should be used with the 'range-root' certificate previously created, and packaged into an application having the correct package name (`com.gsma.iariauth.sample`).

After that, be sure to:
  * Sign the APK with the new `package-signer.jks`
  * Provision with these keys by editing the `profile-ota-prov-iari.xml` file: update the IARI range certificate with the content of `range-root.cert`, then provision the installed stack instances by loading the XML file from the Provisioning launcher on the device
  * Refresh file system in Eclispe (F5)
  * Clean dependant projects


## Generate the security keys and IARI document for a third party application (standalone mode ) ##
  1. Go to the directory `/git/rcsjta/tools/security/tag-auth-util/test`
  1. Edit the file `standalone-sign-test.sh` and verify the declared application package name (`com.gsma.iariauth.sample` in this case).
  1. Run `standalone-sign-test.sh` : this script generates the keys and the IARI document
  1. A new IARI document should be generated in this directory, with a name beginning by `ext.*.xml`.
  1. This extension should be packaged into an application having the correct package name (`com.gsma.iariauth.sample`).

After generating the IARI document, be sure to:
  * Sign the APK with the new `package-signer.jks`

## Generate and sign the test app (SecurityDemoAndroid) ##
  1. Copy to the `assets` of the project's test app's directory (`/git/rcsjta/tools/security/SecurityDemoAndroid/assets`),
    * the generated IARI Authorization XML file
    * and the `range-root-truststore.bks`
  1. Generate the signed test app APK from `SecurityDemoAndroid` project:
    * Better to clean up the project
    * Right click on the project name in the Eclipse's Package Explorer pane and choose `Android Tools/Export signed application package`
    * When asked, choose the `package-signer.jks` keystore (password = `secret`)
  1. Uninstall the test app if needed (force stop from device's `Parameters/application` SecurityDemoAndroid) in order to force its installation
  1. Install the app: from the PC, device is connected, enter `adb install XXXX.apk`.
    * Keep an eye on logcat (filter on RCS for instance): there must be no error or warning
    * Run the test APK. Again: there must be no error or warning. If OK, the app's screen must display a simple:  `result = true`. Everything else would indicate an error.

## Generate and install the stack ##

From Eclipse, in dev mode
  * Better to clean up the project
  * Uninstall the stack from the device (`Parameters/application` / Joyn Service / `Uninstall`)
  * Right click on the project, `Run/Debug as Android app`...

On the device
  * Check the device is connected to the PC via USB
  * Provision the stack if needed:
    * upload the provision XML file. From the PC: `adb push XXXXX.xml /sdcard/`
    * then from the device
  * run the `Provisioning` lancher
  * change `Stack/Configuration mode` to `Manual` then save
  * load the provision XML file: `Profile/Load profile`, choose the provisioning XML file, `Ok`
    * Run the `Settings` launcher,
    * Uncheck the `rcs service` checkbox, then check it to insure the new stack version is running


---

# Tips #
## Important! ##
After each installation of the stack's CORE: always **uncheck then re-check** the _"Joyn service"_ checkbox from _"Joyn settings"_ app to be sure the service is really on.

## Debug the stack ##
... seems to be not possible (TBV)

## Trace the stack ##
From Eclipse
  * Go to the DDMS perspective
  * Click on the chosen device in the Devices' window
  * Right click the project
  * Choose _"Run as Android application"_ to install it on the device
  * Read traces using `logcat`, you can filter with "RCS"  to limit the messages quantity