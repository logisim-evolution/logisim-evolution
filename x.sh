#!/bin/bash

trans-tool --version

# Get all the base files
declare -r base_files=$(find src/main/resources/resources/logisim/* -name "*.properties" | grep -E '*/[a-zA-Z]+.properties$' | grep -v settings.properties)

# Get currently supported languages from settings.properties
declare -r "langs_list=$(cat src/main/resources/resources/logisim/settings.properties | cut -d  ' ' -f3-)"
declare -r langs="${langs_list// /,}"

for f in ${base_files}; do
	#	proptool --config .proptool.ini -b $f
	trans-tool -l ${langs} -b $f
#	echo $f
	read
done
