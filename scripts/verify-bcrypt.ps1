$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
# Use the BCrypt jar that comes with Spring Security (already in m2)
$springBcJar = Get-ChildItem "C:\Users\Khanh\.m2\repository\org\springframework\security" -Recurse -Filter "spring-security-crypto-*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1 -ExpandProperty FullName
if (-not $springBcJar) { throw "spring-security-crypto not found in m2" }

$tmpDir = Join-Path $env:TEMP ("verify-bcrypt-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "VerifyBCrypt.java"

$source = @'
import org.springframework.security.crypto.bcrypt.BCrypt;
public class VerifyBCrypt {
  public static void main(String[] a) {
    String hash = a[0];
    String[] candidates = new String[] { "Demo@123", "admin", "password", "demo", "Demo" };
    System.out.println("hash: " + hash);
    System.out.println("hash length: " + hash.length());
    for (String c : candidates) {
      boolean ok = BCrypt.checkpw(c, hash);
      System.out.println("  " + c + " => " + ok);
    }
  }
}
'@

[System.IO.File]::WriteAllText($javaFile, $source, [System.Text.Encoding]::ASCII)
$prevLoc = (Get-Location).Path
Set-Location $tmpDir
& "$env:JAVA_HOME\bin\javac.exe" -cp $springBcJar $javaFile
if ($LASTEXITCODE -ne 0) { Set-Location $prevLoc; Remove-Item -Recurse $tmpDir; throw "javac failed" }
& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$springBcJar" VerifyBCrypt '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624v8.'
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
