@echo off
setlocal enabledelayedexpansion
set "src=D:\Student Project Registration Page\src\main\resources\templates"
for /r "%src%" %%f in (*.html) do (
    set "file=%%f"
    set "changed=0"
    
    :: Read file into variable
    set "content="
    for /f "usebackq delims=" %%c in ("%%f") do (
        set "line=%%c"
        :: Fix th:replace="fragments/layout :: X" -> th:replace="~{fragments/layout.html :: X}"
        echo !line! | findstr /C:"th:replace=" >nul 2>&1
        if !errorlevel!==0 (
            set "newline=!line:th:replace="fragments/layout :: =th:replace="~{fragments/layout.html :: =!"
            set "newline=!newline:~0,-1!}~"!"
            echo !newline! >> "%%f.tmp"
            set "changed=1"
        ) else (
            echo !line! >> "%%f.tmp"
        )
    )
    
    if "!changed!"=="1" (
        move /y "%%f.tmp" "%%f" >nul 2>&1
        echo Fixed: %%f
    ) else (
        del "%%f.tmp" >nul 2>&1
    )
)
echo Done.
