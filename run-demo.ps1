<#
  Script chạy nhanh app với profile `dev` (H2 in-memory, dữ liệu tự sinh).
  Không cần MySQL. Không cần cấu hình thêm.
  Chạy bằng:  powershell -ExecutionPolicy Bypass -File .\run-demo.ps1
#>

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  HT-QLDT - Chay demo (H2 in-memory)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Kiem tra JAVA_HOME
if (-not $env:JAVA_HOME) {
    # Thu doc JAVA_HOME tu registry (vi setx chi luu vao registry, khong co trong session hien tai)
    $regPath = 'HKCU:\Environment'
    $regVal = Get-ItemProperty -Path $regPath -Name 'JAVA_HOME' -ErrorAction SilentlyContinue
    if ($regVal -and $regVal.JAVA_HOME -and (Test-Path $regVal.JAVA_HOME)) {
        $env:JAVA_HOME = $regVal.JAVA_HOME
        $env:Path = "$env:JAVA_HOME\bin;$env:Path"
        Write-Host "JAVA_HOME (from registry) = $env:JAVA_HOME" -ForegroundColor Yellow
    }
}

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    # Thu tim JDK trong cac duong dan thuong gap
    $candidates = @(
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "C:\Program Files\Java\jdk-17*",
        "C:\Program Files\Microsoft\jdk-17*",
        "C:\Program Files\OpenJDK\jdk-17*"
    )
    $autoDetected = $false
    foreach ($p in $candidates) {
        $found = Get-Item $p -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found -and (Test-Path "$($found.FullName)\bin\java.exe")) {
            $env:JAVA_HOME = $found.FullName
            $env:Path = "$env:JAVA_HOME\bin;$env:Path"
            Write-Host "JAVA_HOME = $env:JAVA_HOME (auto-detected)" -ForegroundColor Yellow
            $autoDetected = $true
            break
        }
    }
    if (-not $autoDetected) {
        Write-Host ""
        Write-Host "[LOI] Khong tim thay Java. Vui long:" -ForegroundColor Red
        Write-Host "   1. Cai JDK 17 tai: https://adoptium.net/" -ForegroundColor Red
        Write-Host "   2. Hoac chay lenh sau (PowerShell Admin):" -ForegroundColor Red
        Write-Host '      [System.Environment]::SetEnvironmentVariable("JAVA_HOME","C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot","User")' -ForegroundColor Red
        Write-Host "   3. Dong terminal, mo lai, chay lai script nay." -ForegroundColor Red
        exit 1
    }
}

# 2. Kiem tra java co chay duoc
Write-Host "PATH check: java.exe o $($env:JAVA_HOME)\bin\java.exe" -ForegroundColor DarkGray
try {
    $javaBin = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (-not (Test-Path $javaBin)) {
        throw "Khong thay file java.exe tai $javaBin"
    }
    # java -version in ra stderr, can redirect ca stdout + stderr qua file tam
    $tmpFile = [System.IO.Path]::GetTempFileName()
    $p = Start-Process -FilePath $javaBin -ArgumentList "-version" -NoNewWindow -Wait -PassThru -RedirectStandardOutput $tmpFile -RedirectStandardError "$tmpFile.err"
    $javaOutput = (Get-Content $tmpFile -Raw) + (Get-Content "$tmpFile.err" -Raw)
    Remove-Item $tmpFile, "$tmpFile.err" -ErrorAction SilentlyContinue
    Write-Host "Java raw output: $($javaOutput.Trim())" -ForegroundColor DarkGray
    $javaVer = ($javaOutput | Select-String -Pattern 'version "(\d+)' | ForEach-Object { $_.Matches[0].Groups[1].Value } | Select-Object -First 1)
    Write-Host "Java version detected: $javaVer" -ForegroundColor DarkGray
    if (-not $javaVer) {
        throw "Khong parse duoc phien ban Java tu output: $javaOutput"
    }
    if ([int]$javaVer -lt 17) {
        Write-Host "[LOI] Can JDK >= 17, hien tai co $javaVer" -ForegroundColor Red
        exit 1
    }
    $firstLine = ($javaOutput -split "`r?`n" | Where-Object { $_.Trim() } | Select-Object -First 1)
    Write-Host "Java OK: $firstLine" -ForegroundColor Green
} catch {
    Write-Host "[LOI] Khong chay duoc java.exe: $_" -ForegroundColor Red
    exit 1
}

# 3. Kiem tra mvnw
if (-not (Test-Path ".\mvnw.cmd")) {
    Write-Host "[LOI] Khong thay mvnw.cmd trong thu muc hien tai." -ForegroundColor Red
    exit 1
}

# 4. Chay app voi profile dev
Write-Host ""
Write-Host "Dang khoi dong app voi profile=dev (H2 in-memory)..." -ForegroundColor Cyan
Write-Host "Sau khi xong, mo: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  admin / admin123         (Quan tri)" -ForegroundColor Green
Write-Host "  truongkhoa / admin123    (Truong khoa)" -ForegroundColor Green
Write-Host "  cthd1 / admin123         (Chu tich hoi dong)" -ForegroundColor Green
Write-Host "  gv_demo / admin123       (Giang vien)" -ForegroundColor Green
Write-Host "  sv_demo / admin123       (Sinh vien)" -ForegroundColor Green
Write-Host ""
Write-Host "Nhan Ctrl+C de dung app." -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Cyan

# Dat profile dev va chay
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
