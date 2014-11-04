#!/usr/bin/sh
if [ -d "$GITHOME/rcsjta/sdk-joyn" ]; then
	if [ -d "$GITHOME/rcsjta.javadoc" ]; then
		cd $GITHOME/rcsjta.javadoc
		git reset --hard ca6cbdf16f13f05a989d271923db7237838adf9d
		rm -Rf $GITHOME/rcsjta.javadoc/sdk-joyn 2> /dev/null
		cp -R $GITHOME/rcsjta/sdk-joyn $GITHOME/rcsjta.javadoc
		git add --all
		git commit -m "Generate javadoc 1.5"
		git push -f origin javadoc1.5
		echo "Doclava created for release 1.5"
	else
		echo "First clone rcsjta.javadoc repository"
	fi
else
	echo "First create doclava for release 1.5"
fi