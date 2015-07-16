usage: IARIValidator [-d <authorization document>] [-n <package name>]
                     [-ps <package signer fingerprint>] [-pk <package
                     signer keystore>] [-pa <package signer certificate
                     alias>] [-pp <package signer keystore password>] -k
                     <keystore> -p <password> [-v]
 -d,--auth-doc <arg>                   Tag authorisation document
 -k,--keystore <arg>                   Path of PKCS12 keystore used to be
                                       sign the widget
 -n,--package-name <arg>               package name
 -p,--passwd <arg>                     Password of end keystore
 -pa,--package-keystore-alias <arg>    package signing certificate alias
 -pk,--package-keystore <arg>          package signing keystore
 -pp,--package-keystore-passwd <arg>   package signing keystore password
 -ps,--package-signer <arg>            package signer fingerprint
 -v,--verbose                          Verbose
