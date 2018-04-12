#!/bin/bash

# This variable is the path where the circ files are located
# can be bassed as argument
LIST_FILE=

# This can be located in the enviroment if not the default path
# will be relative
PATH_LOGISIM=${PATH_LOGISIM:-/../../logisim-evolution.jar}
PATH_CIRC_OUTPUT_FOLDER=${PATH_CIRC_OUTPUT_FOLDER:-/tmp/logisim_xml_gen_output/}

RED="\033[0;31m"
GREEN="\033[0;32m"
NC="\033[0m" # No Color

# Absolute path to this script
SCRIPT=$(readlink -f "$0")

# Absolute path this script is in,
SCRIPTPATH=$(dirname "$SCRIPT")

function print_usage() {
	echo "Usage:"
	echo ""
	echo "This script allow to test circ file generation of logisim"
	echo "		$0 <path_to_folder_containing_circs>"
	echo "		or"
	echo "		$0 <path_to_circ>"
	echo ""
	echo "		-To change path where logisim jar file is loacated:"
	echo "			Set environement var PATH_LOGISIM=<path_logisim_jar>"
	echo "		-To change path output circ file are generated:"
	echo "			Set environement var PATH_CIRC_OUTPUT_FOLDER=<output_folder_path>/logisim_output"
}

function list_files_and_test() {
	LIST_FILE=$(find $PATH_CIRC -name "*.circ")

	# Prepare output folder where the device is 
	# WARNING!! Watchout what path is passed.
	rm -rf $PATH_CIRC_OUTPUT_FOLDER/logisim_output/
	mkdir -p $PATH_CIRC_OUTPUT_FOLDER/logisim_output

	echo "Path output folder > ${PATH_CIRC_OUTPUT_FOLDER}/logisim_output"

	if [[ ! -f $PATH_LOGISIM ]]; then
		printf "${RED} Error: Could not find Logisim, please provide the path\n"
		printf " using PATH_LOGISIM environment var${NC}\n"
		print_usage
		exit 1
	fi

	if [[ "$PATH_LOGISIM" != /* ]]; then
		PATH_LOGISIM=$SCRIPTPATH/${PATH_LOGISIM}
	fi

	# Loop over list of file in folder indicated.
	for CIRC_FILE_INPUT in $LIST_FILE
	do
		# Here we take the basename of the folder remove extension and
		# append _test.circ in the end. In addition the folder to the path
		# where to output the results is appended at the beginning.
		CIRC_FILE_OUTPUT=$(basename $CIRC_FILE_INPUT)
		CIRC_FILE_OUTPUT=$(echo ${CIRC_FILE_OUTPUT} | cut -f 1 -d '.')
		CIRC_FILE_OUTPUT="${PATH_CIRC_OUTPUT_FOLDER}/logisim_output/${CIRC_FILE_OUTPUT}_test.circ"

		# Executing logisim to check generation of file.
		java -jar ${PATH_LOGISIM} -test-circ-gen  ${CIRC_FILE_INPUT}  ${CIRC_FILE_OUTPUT} 

		# Do differences to check.
		diff ${CIRC_FILE_INPUT}  ${CIRC_FILE_OUTPUT}

		# Display results, diff return 0 if everything is ok, 1 or 2 otherwise
		if [ $? -eq 0 ]; then
			printf "xml diff | $CIRC_FILE_INPUT ${GREEN} result: XML GEN SUCCESS${NC}\n"
		else
			printf "xml diff | $CIRC_FILE_INPUT ${RED} result: XML GEN FAILED${NC}\n" 
		fi
	done
}

PATH_CIRC=$1;
list_files_and_test
