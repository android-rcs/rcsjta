#!/bin/bash

# Author:	Tomasz Gawęda - blog.0x1fff.com
# Date:		2009.07.13
# License:	BSD
# Description:	Simple shell script to generate pdf 
#		javadoc (without ANT) from small
#		Java projects - use under Linux.
#
# Dependicies:	Bash
#		Java JDK - 1.6
#		AurigaDoclet jar file (http://sourceforge.net/projects/aurigadoclet/)
#		


# Set the JAVA_HOME variable correctly!!!
# JAVA_HOME="/usr/lib/jvm/java-6-sun-1.6.0.13"
JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")
PATH="$PATH:$JAVA_HOME/bin"
PATH_DOCLET_JAR=pdfdoclet-1.0.2-all.jar
CUSTOM_CONF='conf.properties'

DEFAULT_CONFIG=<<END_DEFAULT_CONFIG
#
# Default config to pdf doclet
#

# Prints @author tags if set to "yes".
tag.author=yes

# Prints @version tags if set to "yes".
tag.version=yes

# Prints @since tags if set to "yes".
tag.since=no

# Show the Summary Tables if set to "yes".
summary.table=yes

# Encrypts the document if set to "yes".
encrypted=yes

# The following property is ignored
# if "encrypted" is not set to yes.
allow.printing=yes

# Creates hyperlinks if set to "yes".
# For print documents, use "no", so
# there will be no underscores.
create.links=yes

# Creates an alphabetical index of all
# classes and members at the end of the
# document if set to "yes".
create.index=yes

# Creates a navigation frame (or PDF
# outline tree) if set to "yes".
create.frame=yes

# Creates a title page at the beginning
# of the document if set to "yes".
api.title.page=yes

# Defines the path of the HTML file that
# should be used as the title page.
#api.title.file=example/laby/laby_title.html

# Defines the title on the title page if
# no external HTML page is used.
api.title=Hello Doclet!

# Defines the copyright text on the
# title page.
api.copyright=Tom

# Defines the author text on the
# title page.
api.author=Tomasz Gawęda

# Defines packages whose classes should not
# be printed fully qualified. For example, every
# Java developer probably knows that "String" is in
# the "java.lang" package, so instead of wasting 
# page spage, just get rid of that package qualifier:
# dontspec=java.lang
dontspec=java.lang,java.io,java.util

#font.text.name=example/fonts/Windows/Vera.ttf
#font.text.name=example/fonts/Windows/Arial.TTF
#font.text.name=example/fonts/garait.ttf
#font.code.name=example/fonts/SF Arch Rival v1.0/SF Arch Rival.ttf
#font.text.enc=Cp1252
#font.text.enc=ISO-8859-9
#font.code.enc=UTF-8

END_DEFAULT_CONFIG

## DO NOT EDIT BELOW THIS LINE ##

########################################################
########################################################
########################################################
########################################################
########################################################
########################################################

# check command line params
if [ $# -lt 2 ] ; then 
	echo usage: $0 src-dir new-pdf-file
	exit 1
fi

if [ ! -r $PATH_DOCLET_JAR ] ; then
	echo "FATAL: Unable to find doclet in $PATH_DOCLET_JAR"
	exit 1
fi

DOCLET_MAIN=com.tarsec.javadoc.pdfdoclet.PDFDoclet
JAVADOC=`which javadoc`
if [ $? -ne 0 ] ; then
	echo "FATAL: Unable to find javadoc in $PATH"
	exit 1
fi
echo "JavaDoc found in $JAVADOC"

SRCDIR=$1
if [ ! -d $SRCDIR ] ; then
	echo "FATAL: Directory $SRCDIR does not exists"
	exit 1
fi
echo "Src directory of program exists ($SRCDIR)"

OUTPUT=$2
if [ -e $OUTPUT ] ; then
	echo "WARNING: Out document exists - will be overwritten ($OUTPUT)"
	echo "WARNING: Got five seconds to break"
	sleep 3
	echo "WARNING: Got two seconds to break"
	sleep 2
	echo "WARNING: Too late ..."
fi

DEL_CONF=0
echo "Checking if exists custom configuration ($CUSTOM_CONF)"
if [ ! -r $CUSTOM_CONF ] ; then
	echo "Custom configuration doesn't exists using default"
	echo $DEFAULT_CONFIG > $CUSTOM_CONF
	DEL_CONF=1
fi

echo "Starting analyzing sources - finding packages"
# find all packages
PACKAGES=`find $SRCDIR -type d -and -not -empty|sed s#$SRCDIR##g | sed s#/#.#g`
echo "Found "`echo $PACKAGES | wc -l `" packages"

export JAVA_HOME PATH DOCLET_MAIN PATH_DOCLET_JAR JAVADOC

echo "Running JavaDoc"
set -x
# Run
$JAVADOC -doclet $DOCLET_MAIN -docletpath $PATH_DOCLET_JAR -pdf $OUTPUT -config $CUSTOM_CONF -sourcepath $SRCDIR $PACKAGES
set +x

echo "JavaDoc finished with exit status "$?

if [ $DEL_CONF ] ; then
	echo "Cleanning up"
	rm $CUSTOM_CONF
fi

