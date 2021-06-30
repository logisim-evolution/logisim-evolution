for /r "1616" %%a in (.) do (
  pushd "%%a"
    for /f "delims=" %%i in ('dir *.png /b/a-d/l') do ren "%%~fi" "%%i"
  popd)