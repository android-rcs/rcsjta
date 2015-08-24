# Build the RCS stack #

Clone a GIT branch of the project:
> _git clone https://github.com/android-rcs/rcsjta.git -b <a branch>_

From a console, build the third party libraries (DNS, NIST-SIP and BouncyCastle) via the command line:
> _ant libs_

From Eclipse, create a new project from existing source code and select the folder /core of your branch.

To be backward compatible with older SDK releases, you should select the SDK defined in the target variable of the project.properties file. 

# Build the Media player library #

From Eclipse, create a new project from existing source code and select the folder /mediaplayer of your branch.

# Build the RI application #

From a console, build the RCS API library and the media player library used by the Video sharing service. Via the command line in the folder /RI, enter:
> _ant -f build-local.xml libs_ (rcs\_api.jar and rcs\_media.jar + .so codecs libs are produced in the directory ./libs)

From Eclipse, create a new project from existing source code and select the folder /RI of your branch.

# Build the RCS core control application #

From a console, build the RCS API library and connection manager library via the command line in the /tools/core_control folder:
> _ant -f build-local.xml libs_ (rcs\_api.jar and rcs\_cnx.jar are produced in the directory ./libs)

From Eclipse, create a new project from existing source code and select the folder /tools/core_control of your branch.

# Build the Javadoc #
Define the ANDROID_TARGET environment variable to be used to generate the SDK. Example:
> _export ANDROID_TARGET=android-19_

From a console, build the Javadoc of the RCS API via the command line in the root folder of your branch:
> _ant docs_ (the doc is produced in the directory ./doclava)

# Build the SDK #

From a console, build the RCS SDK via the command line:
> _ant sdk_ (the SDK is produced in the directory ./gen)


# Apply the same source code format #

The formatting guidelines are in the file called android-formatting-rules.xml in the _eclipse_ directory. There are also as set of rules of how to order the imports in the file called android.importorder.

From Eclipse and the menu Preferences/Java/Code Style/Formatter, import the file /eclipse/android-formatting from your branch.

From Eclipse and the menu Preferences/Java/Code Style/Organize Imports, import the file /eclipse/android.importorder from your branch.
