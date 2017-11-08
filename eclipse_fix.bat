@echo off

REM need administrator access to run mklink!

cd bin

mklink /D boards_model ..\boards_model
mklink /D javax ..\javax
mklink /D libs ..\libs
mklink /D resources ..\resources
mklink /D doc ..\doc

cd ..
