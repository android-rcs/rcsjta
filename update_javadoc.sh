#!/usr/bin/sh
if [ -d "$GIT_HOME/rcsjta/doclava" ]; then
	if [ -d "$GIT_HOME/rcsjta.javadoc" ]; then
		cd $GIT_HOME/rcsjta.javadoc
		git reset --hard 9d755aa6730a89fa054b38028eab1c064379ed2a
		rm -Rf $GIT_HOME/rcsjta.javadoc/reference 2> /dev/null
		cp -R $GIT_HOME/rcsjta/doclava/reference $GIT_HOME/rcsjta.javadoc
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