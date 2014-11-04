#!/usr/bin/sh
if [ -d "$GITHOME/rcsjta/sdk-joyn" ]; then
	if [ -d "$GITHOME/rcsjta.javadoc" ]; then
		cd $GITHOME/rcsjta.javadoc
		git reset --hard 9d755aa6730a89fa054b38028eab1c064379ed2a
		rm -Rf $GITHOME/rcsjta.javadoc/reference 2> /dev/null
		cp -R $GITHOME/rcsjta/doclava/reference $GITHOME/rcsjta.javadoc
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