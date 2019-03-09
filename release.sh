#!/bin/bash
#Arguments:
#	-v version eg 6.0.3
#	-m 6.0.3
set -e

while getopts v:m: OPTION;
do
case $OPTION 
	in
	v)      VERSION=$OPTARG;;
    m)	    MESSAGE=$OPTARG;;
esac
done

# Show error message if no version was provided
if [[ -z ${VERSION} ]];
then
    echo "UsbSerial: Error!! No version was provided"
    exit 0
fi

# Show error message if no message was provided
if [[ -z ${MESSAGE} ]];
then
    echo "UsbSerial: Error!! No message was provided"
    exit 0
fi

echo "UsbSerial: Starting Release $VERSION with commit message $MESSAGE"

VERSION_NAME="VERSION_NAME=$VERSION"

# Updating gradle.properties with version
ex -sc '1d|x' gradle.properties
ex -sc "1i|$VERSION_NAME" -cx gradle.properties

# Updating README file
GRADLE_LINE="implementation 'com.github.felHR85:UsbSerial:${VERSION}'"
LINE=$(cat README.md | grep -nr implementation\ \'com.github.felHR85:UsbSerial: | awk -F ":" '{print $2}')
ex -sc "${LINE}d|x" README.md
ex -sc "${LINE}i|$GRADLE_LINE" -cx README.md

# Gradle clean and build
./gradlew clean build

# Git stuff
git add .
git commit -m "${MESSAGE}"
git tag ${VERSION}
git push origin master
git push --tags

echo "UsbSerial: Release Finished!!!"