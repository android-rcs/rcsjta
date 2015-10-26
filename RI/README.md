# The Reference Implementation

This application shows how to use the RCSJTA api.<br>
It gives examples of use cases for a RCSJTA client application.

Build instruction:

<code>../gradlew :RI:build</code>

**Additional steps for Eclipse compatibility:**

<code>ant libs</code>

This will create the following jar files under the "libs" folder:
- api.jar
- rcs_media.jar
- api_cnx.jar

Download the [android-support-v4.jar](https://developer.android.com/tools/support-library/setup.html) library and copy under the "libs" folder.

Set up the [google play service library](https://developers.google.com/android/guides/setup).

Set up the api connection library by importing the Android project '../libs/api_cnx/build/intermediates/bundles/debug/'
