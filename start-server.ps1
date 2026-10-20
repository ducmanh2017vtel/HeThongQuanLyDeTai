$ErrorActionPreference = 'Continue'
$env:APP_HOME = 'D:\Student Project Registration Page'
Set-Location 'D:\Student Project Registration Page'
$log = 'D:\Student Project Registration Page\server.log'
$err = 'D:\Student Project Registration Page\server-err.log'
$proc = Start-Process -FilePath 'java' -ArgumentList '-jar','target/hethong-quanlydetai.jar','--spring.profiles.active=dev' -PassThru -RedirectStandardOutput $log -RedirectStandardError $err -WindowStyle Hidden
Start-Sleep -Seconds 3
Write-Host "PID=$($proc.Id)"
