$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$springBcJar = Get-ChildItem "C:\Users\Khanh\.m2\repository\org\springframework\security" -Recurse -Filter "spring-security-crypto-*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1 -ExpandProperty FullName
$commLog = Get-ChildItem "C:\Users\Khanh\.m2\repository\commons-logging" -Recurse -Filter "commons-logging-*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1 -ExpandProperty FullName
$mysqlJar = "C:\Users\Khanh\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar"
$cp = "$springBcJar;$commLog"

$url  = if ($env:DB_URL)      { $env:DB_URL }      else { "jdbc:mysql://localhost:3306/hethong_quanlydetai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&useUnicode=true" }
$user = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

$tmpDir = Join-Path $env:TEMP ("reset-pw-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "ResetPw.java"

$source = @'
import java.sql.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class ResetPw {
  public static void main(String[] a) throws Exception {
    String url  = a[0];
    String user = a[1];
    String pass = a[2];
    String plain = a[3];
    BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
    String hash = "{bcrypt}" + enc.encode(plain);
    System.out.println("new hash: " + hash);
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection(url, user, pass);
         PreparedStatement upd = c.prepareStatement("UPDATE users SET password=?")) {
      upd.setString(1, hash);
      int n = upd.executeUpdate();
      System.out.println("updated rows: " + n);
    }
  }
}
'@

[System.IO.File]::WriteAllText($javaFile, $source, [System.Text.Encoding]::ASCII)
$prevLoc = (Get-Location).Path
Set-Location $tmpDir
& "$env:JAVA_HOME\bin\javac.exe" -cp $cp $javaFile
if ($LASTEXITCODE -ne 0) { Set-Location $prevLoc; Remove-Item -Recurse $tmpDir; throw "javac failed" }
& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$cp;$mysqlJar" ResetPw $url $user $pass "Demo@123"
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
