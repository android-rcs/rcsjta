# Build the RCS stack #

Clone a GIT branch of the project:
> _git clone https://code.google.com/p/rcsjta/ -b <a branch>_

From a console, build the third party libraries (DNS, NIST-SIP and BouncyCastle) via the command line:
> _ant libs_

From Eclipse, create a new project from existing source code and select the folder /core of your branch.


# Build the RI application #

From a console, build the RCS API library (rcs\_api.jar) via the command line:
> _ant api_ (the produced JAR file is automatically added to the RI app and samples)

Build the media player used by the Video sharing service, see next chapter.

From Eclipse, create a new project from existing source code and select the folder /RI of your branch.

# Build the Media player library #

From Eclipse, create a new project from existing source code and select the folder /mediaplayer of your branch.

From a console, build the RCS media library (rcs\_api.jar and .so codecs libs) via the command line:
> _ant media_ (the produced JAR file is automatically added to the RI app)


# Build the Javadoc #

From a console, build the Javadoc of the RCS API via the command line:
> _ant docs_ (the doc is produced in the directory ./doclava)



# Build the SDK #

From a console, build the RCS SDK via the command line:
> _ant sdk_ (the SDK is produced in the directory ./sdk-joyn)


# Apply the same source code format #

The formatting guidelines are in the file called android-formatting-rules.xml in the _eclipse_ directory. There are also as set of rules of how to order the imports in the file called android.importorder.

From Eclipse and the menu Preferences/Java/Code Style/Formatter, import the file /eclipse/android-formatting from your branch.

From Eclipse and the menu Preferences/Java/Code Style/Organize Imports, import the file /eclipse/android.importorder from your branch.