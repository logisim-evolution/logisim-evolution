#!/bin/bash

# Absolute path to this script
SCRIPT=$(readlink -f "$0")

# Absolute path this script is in
SCRIPTPATH=$(dirname "$SCRIPT")

CIRC_FILES=
MAP_FILES=
CIRCUIT_TOPLEVEL_NAME=
BOARDS_NAME=
BOARDS_AVAILABLE=

RED="\033[0;31m"
GREEN="\033[0;32m"
BOLD="\033[1m"
NC="\033[0m" # No Color

# To be adapted
PATH_LOGISIM="${SCRIPTPATH}/../../logisim-evolution.t.jar"

function print_usage() {
	echo "Usage:"
	echo ""
	echo "This script allow to test synthesis of circuit from logisim design
		on a specific board."
	echo "		$0 <path_to_configuration_file>"
	echo "		or"
	echo "		$0 <path_to_circ>"
	exit -1
}

function check_number() {
	if [ ${#CIRC_FILES[@]} -ne ${#MAP_FILES[@]} ]; then
		echo "Missing Map file information"
		print_usage
	fi

	if [ ${#CIRC_FILES[@]} -ne ${#CIRCUIT_TOPLEVEL_NAME[@]} ]; then
		echo "Missing Circuit name"
		print_usage
	fi

	if [ ${#CIRC_FILES[@]} -ne ${#BOARDS_NAME[@]} ]; then
		echo "Missing Board Name"
		print_usage
	fi
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

function check_boards_exists() {
	tLen=${#BOARDSS_NAME[@]}

	BOARDS_AVAILABLE=($(jar tf ${PATH_LOGISIM} | \
		grep "resources/logisim/boards/" | \
		cut -f8 | cut -d \/ -f4 | cut -d \. -f1))

	tLen_boards=${#BOARDS_NAME[@]}
	tLen=${#BOARDS_AVAILABLE[@]}

	for (( j=0; j<${tLen_boards}; j++ ));
	do
		for (( i=0; i<${tLen}; i++ ));
		do
			if [[ ${BOARDS_NAME[$j]} == ${BOARDS_AVAILABLE[$i]} ]]; then
				break
			fi
		done
# Here we check if the board was found
		if [ $i -eq $tLen ]; then
			printf "${RED}Could not find board \"${BOARDS_NAME[$i]}\" line $((j + 1)) ${NC}\n"
		fi
	done
}

function read_form_file() {
	CIRC_FILES=($(cat $PATH_CIRC_DESCRIPTOR  | cut  -d \; -f1))
	MAP_FILES=($(cat $PATH_CIRC_DESCRIPTOR  | cut  -d \; -f2))
	CIRCUIT_TOPLEVEL_NAME=($(cat $PATH_CIRC_DESCRIPTOR  | cut  -d \; -f3))
	BOARDS_NAME=($(cat $PATH_CIRC_DESCRIPTOR  | cut  -d \; -f4))
}

function start_tests()
{
	tLen=${#CIRC_FILES[@]}

	for (( i=0; i<${tLen}; i++ ));
	do
		printf "\nTesting Configuration $((i + 1)):\n\tFile: ${BOLD}$(basename ${CIRC_FILES[i]})${NC}"
		printf "\n\tMapping: ${BOLD}$(basename ${MAP_FILES[i]})${NC}"
		printf "\n\tCircuit: ${BOLD}${CIRCUIT_TOPLEVEL_NAME[i]}${NC}"
		printf "\n\tBoard: ${BOLD}${BOARDS_NAME[i]}${NC}"
		printf "\n\tResults --> "
		java -jar $PATH_LOGISIM -test-fpga-implementation ${CIRC_FILES[i]} \
			${MAP_FILES[i]} ${CIRCUIT_TOPLEVEL_NAME[i]} ${BOARDS_NAME[i]} > "file_imp_test_$((i + 1))".log 2>&1

		# Display results, diff return 0 if everything is ok, 1 or 2 otherwise
		if [ $? -eq 0 ]; then
			printf "${GREEN}Implementation Pass${NC}\n"
		else
			printf "${RED}Implementation Failed${NC}\n" 
		fi
	done
}

# Check if enough param were given
if [ $# -ne 1]; then
	print_usage
fi

PATH_CIRC_DESCRIPTOR=$1;

# Read the data
read_form_file
# Check number of circ file matches number of map/circuit/board
check_number
# Check if boards exists
check_boards_exists

# Check path of files
check_files_exits ${CIRC_FILES[@]}
check_files_exits ${MAP_FILES[@]}

start_tests
