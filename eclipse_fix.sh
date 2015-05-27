# Creates the links which are missing and prevent Eclipse to successfully execute
# Logisim

#!/bin/bash

mkdir -p bin
cd bin
ln -s ../boards_model .
ln -s ../javax .
ln -s ../libs .
ln -s ../resources .
ln -s ../doc .
cd ..
