# Creates the links which are missing and prevent Eclipse to successfully execute
# Logisim

#!/bin/bash

ln -s ${PWD}/boards_model ${PWD}/bin/boards_model
ln -s ${PWD}/javax ${PWD}/bin/javax
ln -s ${PWD}/libs ${PWD}/bin/libs
ln -s ${PWD}/resources ${PWD}/bin/resources
ln -s ${PWD}/doc ${PWD}/bin/doc
