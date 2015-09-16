# IMS notification manager

This application manages a notification in the Android status bar to display the current IMS connection status.

##Build instruction:

<code>../../gradlew :ims_notification:build</code>

Additional steps for Eclipse compatibility:

<code>ant libs</code>

This will create the following jar files under the "libs" folder:
- api_lib.jar

Download the [android-support-v4.jar](https://developer.android.com/tools/support-library/setup.html) library and copy under the "libs" folder. 