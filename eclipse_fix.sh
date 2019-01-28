# Creates the links which are missing and prevent Eclipse to successfully execute
# Logisim

#!/bin/bash

mkdir -p bin
cd bin
ln -s ../boards_model .
ln -s ../src/main/resources/javax .
ln -s ../lib .
ln -s ../lib-emf .
ln -s ../src/main/resources/resources .
ln -s ../src/main/resources/doc .
cd ..
