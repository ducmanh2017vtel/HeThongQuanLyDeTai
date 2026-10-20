$ErrorActionPreference = 'Stop'
$src = 'D:\Student Project Registration Page\src\main\resources\templates'
$oldPattern = 'th:replace="fragments/layout :: '
$newPrefix = 'th:replace="~{fragments/layout.html :: '
$newSuffix = '}"'

$count = 0
Get-ChildItem -Path $src -Filter '*.html' -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match [regex]::Escape($oldPattern)) {
        $newContent = $content -replace "th:replace=`"fragments/layout :: ([^`"]+)`"",
                               'th:replace="~{fragments/layout.html :: $1}"'
        Set-Content -Path $_.FullName -Value $newContent -NoNewline
        Write-Host "Fixed: $($_.FullName)" -ForegroundColor Cyan
        $count++
    }
}
Write-Host "Total fixed: $count files" -ForegroundColor Green
