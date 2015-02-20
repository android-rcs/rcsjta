Instructions to format source files according to android rules:
(Reference: https://android.googlesource.com/platform/development/+/master/ide/eclipse/)

The formatting guidelines are in the file called android-formatting-rules.xml in the eclipse directory.
There are also as set of rules of how to order the imports in the file called android.importorder.

1- Import it with Eclipse
Menu bar -> Window -> Preferences -> Java -> Code Style -> Formatter -> Import
Point it to the eclipse folder and select the android-formatting.xml

Menu bar -> Window -> Preferences -> Java -> Code Style -> Organize Imports -> Import
Point it to the eclipse folder and select the android.importorder

2- Format source files
2-1 Command line option
Define the ECLIPSE environment variable to the excutable path then enter the below command:
prompt> ./eclipse/rcsjta-format

2-2 Eclipse GUI option
Right click on the source folder then select "Source -> Format"

Once formatted, a source file should not be changed if you reformat it manually using the <CTRL>+A (Select all) then <CTRL>+F (Format) sequences.

Ensure that XML, AIDL and java files are converted to UNIX text file format (Line delimiter: LF)
find <source directory> -name "*.java" -exec dos2unix {} \;
find <source directory> -name "*.aidl" -exec dos2unix {} \;
find <resource directory> -name "*.xml" -exec dos2unix {} \;

All committed files MUST be formatted according to above rules.
