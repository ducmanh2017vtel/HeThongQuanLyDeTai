$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$mysqlJar = "C:\Users\Khanh\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar"

$url  = if ($env:DB_URL)      { $env:DB_URL }      else { "jdbc:mysql://localhost:3306/hethong_quanlydetai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&useUnicode=true" }
$user = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

$tmpDir = Join-Path $env:TEMP ("inspect-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "Inspect.java"

$source = @'
import java.sql.*;
public class Inspect {
  public static void main(String[] a) throws Exception {
    String url  = a[0];
    String user = a[1];
    String pass = a[2];
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection(url, user, pass);
         Statement s = c.createStatement()) {
      try (ResultSet rs = s.executeQuery(
        "SELECT id, username, status, enabled, failed_attempts, locked_until FROM users " +
        "WHERE username IN ('admin','truongkhoa','gv_demo','sv_demo') " +
        "ORDER BY username")) {
        while (rs.next()) {
          System.out.printf("%-12s status=%-7s enabled=%-5s failed=%-3s locked_until=%s%n",
            rs.getString(2), rs.getString(3), rs.getBoolean(4),
            rs.getObject(5), rs.getString(6));
        }
      }
      int n = s.executeUpdate("UPDATE users SET failed_attempts=0, locked_until=NULL, status='ACTIVE', enabled=1 WHERE username='admin'");
      System.out.println("admin reset rows: " + n);
    }
  }
}
'@

[System.IO.File]::WriteAllText($javaFile, $source, [System.Text.Encoding]::ASCII)
$prevLoc = (Get-Location).Path
Set-Location $tmpDir
& "$env:JAVA_HOME\bin\javac.exe" -cp $mysqlJar $javaFile
if ($LASTEXITCODE -ne 0) { Set-Location $prevLoc; Remove-Item -Recurse $tmpDir; throw "javac failed" }
& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$mysqlJar" Inspect $url $user $pass
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
