#!/bin/bash
#TODO: Update README.md

while getopts v:m: OPTION;
do
case $OPTION 
	in
	v)      VERSION=$OPTARG;;
    m)	    MESSAGE=$OPTARG;;
esac
done

echo "Starting Release $VERSION with commit message $MESSAGE"

VERSION_NAME="VERSION_NAME=$VERSION\n"

ex -sc "1i|$VERSION_NAME" -cx gradle.properties
./gradlew clean build
git commit -am $MESSAGE
git tag -a $VERSION
git push && git push --tags