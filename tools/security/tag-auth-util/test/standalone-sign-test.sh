#!/bin/sh
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#cd $DIR

# User parameters definition
export SECRET="secret"
export EXTENSION="messaging-standalone-ext0"
export PKG_NAME="com.gsma.iariauth.sample"

# echo remove old keys etc
rm -f keys/$EXTENSION*

# generate new standalone iari
echo "create standalone iari"
java -jar ../build/iaritool.jar -generate -keyalg RSA -alias $EXTENSION -keystore keys/$EXTENSION.jks -storepass $SECRET -keypass $SECRET -dname CN=iari.standalone.test -validity 360 -keysize 2048 -dest $EXTENSION.xml -v

keytool -importkeystore -srckeystore keys/$EXTENSION.jks -destkeystore keys/$EXTENSION.bks -srcstoretype JKS -deststoretype BKS -srcstorepass $SECRET -deststorepass $SECRET -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath ../libs/bcprov-jdk15on-150.jar


# When saving keys in store, the iaritool used a temporary file (with inprogress extension)
# It can be necessary to rename this temporary file if the iaritool didn't succeed in making it
JKS_FILE=keys/$EXTENSION.jks 
if [ -f $JKS_FILE.inprogress ]; then
  echo ""
  echo "WARNING : In progress file exists, a renaming is required"
  echo "Deleting $JKS_FILE ..."
  rm -f $JKS_FILE;
  echo "Renaming $JKS_FILE.inprogress in $JKS_FILE ..."
  mv $JKS_FILE.inprogress $JKS_FILE
  echo ""
fi

# sign package with that iari
echo "create iari authorization for package"
java -jar ../build/iaritool.jar -sign -template $EXTENSION.xml -dest $EXTENSION.xml -alias $EXTENSION -keystore keys/$EXTENSION.jks -storepass $SECRET -keypass $SECRET -pkgname $PKG_NAME -pkgkeystore keys/package-signer.jks -pkgalias package-signer -pkgstorepass $SECRET -v


# validate auth document
echo "validate signed iari authorization"
java -jar ../../tag-auth-validator/build/iarivalidator.jar -d $EXTENSION.xml -pkgname $PKG_NAME -keystore keys/$EXTENSION.jks -storepass $SECRET -pkgkeystore keys/package-signer.jks -pkgalias package-signer -pkgstorepass $SECRET -v

# extract generated extension from IARI document
GENERATED_EXTENSION=`sed -nr 's/^.*urn:urn-7:3gpp-application.ims.iari.rcs.(.*)<\/iari>$/\1/p' $EXTENSION.xml`
mv $EXTENSION.xml $GENERATED_EXTENSION.xml
