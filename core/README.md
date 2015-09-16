#RCS core stack
Build instruction:

<code>../gradlew :core:build</code>

Additional steps for Eclipse compatibility:

<code>ant libs</code>

This will create the following jar files under the "libs" folder:
- bouncycastle-xxx.jar
- api_lib.jar
- nist_sip.jar

Download the [dnsjava-2.1.7.jar](http://mvnrepository.com/artifact/dnsjava/dnsjava/2.1.7) library and copy under the "libs" folder. 
