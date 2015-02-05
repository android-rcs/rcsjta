Instructions to format source files according to android rules:

The formatting guidelines are in the file called eclipse-android-formatting-rules.xml in the current directory.

1- Import it with Eclipse
Menu bar -> Window -> Preferences -> Java -> Code Style -> Formatter -> Import
Point it to the path of the eclipse-android-formatting-rules.xml

2- Format source files
2-1 Command line option
prompt> eclipse -application org.eclipse.jdt.core.JavaCodeFormatter -config <path to org.eclipse.jdt.core.prefs > <path to source code >

2-2 Eclipse GUI option
Right click on the source folder then select "Source -> Format"

Once formatted, a source file should not be changed if you reformat it manually using the <CTRL>+A (Select all) then <CTRL>+F (Format) sequences.

3. Ensure that XML, AIDL and java files are converted to UNIX text file format (Line delimiter: LF)
find <source directory> -name "*.java" -exec dos2unix {} \;
find <source directory> -name "*.aidl" -exec dos2unix {} \;
find <resource directory> -name "*.xml" -exec dos2unix {} \;

All committed files MUST be formatted according to above rules.
