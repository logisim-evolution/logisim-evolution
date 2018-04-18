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

function get_list_of_files()
{
	CIRC_FILE=$(find $PATH_CIRC -name "*.circ")
}

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
		printf "\nSimulation testing file ${CIRC_FILE[i]}: "
		java -jar $PATH_LOGISIM -test-circuit ${CIRC_FILES[i]} \
			> "file_sim_test_$((i + 1))".log 2>&1

		if [ $? -eq 0 ]; then
			printf "${GREEN}SUCCESS${NC}"
		else
			printf "${RED}FAILED${NC}"
		fi
	done
}

if [ $# -ne 1 ]; then
	print_usage
fi

if [[ "$1" == "-h" ]]; then
	print_usage
fi


get_list_of_files $1
check_files_exits
start_tests
