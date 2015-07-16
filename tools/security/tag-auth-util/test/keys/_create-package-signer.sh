#!/bin/sh
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#cd $DIR

export SECRET="secret"

# remove existing keys
rm -f range* package* com*
rm -f *standalone*

# create package signer cert and key.
echo "create package signer cert and key"
echo "1 -"
keytool -genkey -keyalg RSA -alias package-signer -keystore package-signer.jks -storepass $SECRET -keypass $SECRET -dname CN=package-signer-ext -validity 360 -keysize 2048
echo "2 -"
keytool -list -keystore package-signer.jks -storepass $SECRET | grep fingerprint

