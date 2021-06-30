for /r "%~1" %%a in (.) do (
  pushd "%%a"
    for /f "delims=" %%i in ('dir *.flac /b/a-d/l') do ren "%%~fi" "%%i"
  popd)