# Android Studio

How to set up the project under Android Studio.

* Check that your JDK location is **JDK 1.7** (*File -> Project Structure*)
* From Android Studio menu "*File -> New -> Import Project*" select the root directory of your RCSJTA GIT branch.

**Warning: under Android Studio you need also to remove the extension"rcs" or "RCS" from the list "Ignore files and folder" in the menu "Settings / Editor / File Types".**

<img src='https://github.com/android-rcs/rcsjta/blob/master/studio/ignored.jpg'>


* Import the Eclipse code formatter plugin to format Java code accroding to project's rules defined in the directory *eclipse*.

From "*File -> Settings*" menu choose *Plugins* and click "*-> Install JetBrains plugin...*". Find and install the plugin "Eclipse Code Formatter".

After installation plugin, select "*File -> Settings*" again and navigate to "*Other Settings -> Eclipse Code Formatter*" to enable "Use the Eclipse code formatter". 

In "Eclipse java Formatter config file" click browse and find path to "**eclipse/android-eclipse-formatting.xml**". After configuration press Apply.

Finally edit the manual configuration for "*Import order*" to set "**com.gsma;com;dalvik;gov;junit;libcore;net;org;java;javax**" and click Enable.

Now you can use formatter in file editor: CTRL+ALT+L.

The "Eclipse Code Formatter" configuration in the "Settings" window should look like this:

<img src='https://github.com/android-rcs/rcsjta/blob/master/studio/AndroidStudioFormatCode.jpg'>
