#!/bin/bash

# Absolute path to this script
SCRIPT=$(readlink -f "$0")

# Absolute path this script is in
SCRIPTPATH=$(dirname "$SCRIPT")

# List of circ files
CIRC_FILES=

# Colors
RED="\033[0;31m"
GREEN="\033[0;32m"
BOLD="\033[1m"
NC="\033[0m" # No Color

# To be adapted
PATH_LOGISIM="${SCRIPTPATH}/../../logisim-evolution.t.jar"

function check_files_exits() {
	TAB=(${@})
	tLen=${#TAB[@]}


	for (( i=0; i<${tLen}; i++ ));
	do
		if [ ! -f ${TAB[$i]} ]; then
			printf "${RED}Could not find path ${TAB[$i]} line $((i + 1)) ${NC}\n"
			exit -1
		fi
	done
}

function start_tests()
{
	tLen=${#CIRC_FILES[@]}

	for (( i=0; i<${tLen}; i++ ));
	do
		java -jar $PATH_LOGISIM -test-fpga-implementation ${CIRC_FILES[i]} \
			${MAP_FILES[i]} ${CIRCUIT_TOPLEVEL_NAME[i]} ${BOARDS_NAME[i]} \
			> "file_sim_test_$((i + 1))".log 2>&1
	done
}

get_list_of_files
check_files_exits
start_tests
