$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$jar = "C:\Users\Khanh\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar"

$url  = if ($env:DB_URL)      { $env:DB_URL }      else { "jdbc:mysql://localhost:3306/hethong_quanlydetai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&useUnicode=true" }
$user = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

$tmpDir = Join-Path $env:TEMP ("check-pw-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "CheckPw.java"

$source = @'
import java.sql.*;
public class CheckPw {
  public static void main(String[] a) throws Exception {
    String url  = a[0];
    String user = a[1];
    String pass = a[2];
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection(url, user, pass);
         Statement s = c.createStatement()) {
      try (ResultSet rs = s.executeQuery(
        "SELECT username, SUBSTRING(password,1,7) AS pw_prefix, LENGTH(password) AS pw_len FROM users WHERE username IN ('admin','truongkhoa','gv_demo','sv_demo')")) {
        while (rs.next()) {
          System.out.println(rs.getString(1) + "  prefix=" + rs.getString(2) + "  len=" + rs.getString(3));
        }
      }
    }
  }
}
'@

[System.IO.File]::WriteAllText($javaFile, $source, [System.Text.Encoding]::ASCII)
$prevLoc = (Get-Location).Path
Set-Location $tmpDir
& "$env:JAVA_HOME\bin\javac.exe" $javaFile
if ($LASTEXITCODE -ne 0) { Set-Location $prevLoc; Remove-Item -Recurse $tmpDir; throw "javac failed" }
& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$jar" CheckPw $url $user $pass
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
