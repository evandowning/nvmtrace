:: Renew the DHCP lease
ipconfig /renew

:: Variables for folder paths
set "gnu=C:\Program Files\GnuWin32\bin"
::set "gnu=C:\Program Files (x86)\GnuWin32\bin"
set "desktop=%USERPROFILE%\Desktop"

:: Variables for output files
set "server=%desktop%\server"
set "exe=%desktop%\exe"

:: Output IP address of default gateway and the name of the executable to download to files
ipconfig | "%gnu%\grep" "Default Gateway" | "%gnu%\mawk" "{print $NF}" > "%server%"
ipconfig | "%gnu%\grep" "Default Gateway" | "%gnu%\mawk" "{print $NF}" | "%gnu%\mawk" -F "." "{print $3}" > "%exe%"

:: Retrieve IP address of default gateway and the name of the executable to download from files
set /p ip=<"%server%"
set /p run=<"%exe%"

:: Delete output files
del "%server%"
del "%exe%"

:: Download analyzer to run
"%gnu%\wget" http://%ip%/analyzer.zip -O "%desktop%\analyzer.zip"

:: Unzip analyzer
"%gnu%\unzip" "%desktop%\analyzer.zip" -d "%desktop%"

:: Delete zip file
del "%desktop%\analyzer.zip"

:: Download executable to run
"%gnu%\wget" http://%ip%/%run% -O "%desktop%\artifact"

:: Run analyzer
call "%desktop%\windows-analyzer\run.bat"

:: Upload results (or whatever the analyzer wants to do)
call "%desktop%\windows-analyzer\finish.bat"
