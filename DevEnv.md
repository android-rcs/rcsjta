#Prerequisites
Configure the ANDROID_HOME environment variable based on the location of the Android SDK. 

Also add the ANDROID_HOME/tools and ANDROID_HOME/platform-tools to your PATH. 

#Build the whole SDK

Clone a GIT branch of the project:
> _git clone https://github.com/android-rcs/rcsjta.git -b <a branch>_

From a console, list of tasks of the RCSJTA project via the command line:
> _gradlew tasks_

From a console, list all projects via the command line:
> _gradlew projects_

From a console, build all projects via the command line:
> _gradlew build_

From a console, install all projects via the command line:
> _gradlew installDebug_

From a console, build the RCS SDK via the command line:
> _ant sdk_ (the SDK is produced in the directory ./gen)

# Build the RCS stack only

From a console, enter the command:
> _gradlew :core:build_

# Build the RI application only

From a console, enter the command:
> _gradlew :RI:build_

# Build the RCS core settings application only

From a console, enter the command:
> _gradlew :settings:build_

# Build the Javadoc only
From a console, enter the command:
> _gradlew javadoc_ (the javadoc is produced in the directory ./libs/api/build/docs/javadoc)

# Apply the same source code format #

The formatting guidelines are in the file called android-formatting-rules.xml in the _eclipse_ directory. There are also as set of rules of how to order the imports in the file called android.importorder.

From Eclipse and the menu Preferences/Java/Code Style/Formatter, import the file /eclipse/android-formatting from your branch.

From Eclipse and the menu Preferences/Java/Code Style/Organize Imports, import the file /eclipse/android.importorder from your branch.
