$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$jar = "C:\Users\Khanh\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar"

$url  = if ($env:DB_URL)      { $env:DB_URL }      else { "jdbc:mysql://localhost:3306/hethong_quanlydetai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&useUnicode=true" }
$user = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

$tmpDir = Join-Path $env:TEMP ("check-users-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "CheckUsers.java"

$source = @'
import java.sql.*;
public class CheckUsers {
  public static void main(String[] a) throws Exception {
    String url  = a[0];
    String user = a[1];
    String pass = a[2];
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection(url, user, pass);
         Statement s = c.createStatement()) {
      try (ResultSet rs = s.executeQuery(
        "SELECT u.id, u.username, u.full_name, u.status, u.enabled, " +
        " GROUP_CONCAT(DISTINCT r.name ORDER BY r.name SEPARATOR ',') AS roles " +
        " FROM users u LEFT JOIN user_roles ur ON u.id=ur.user_id " +
        " LEFT JOIN roles r ON ur.role_id=r.id " +
        " GROUP BY u.id, u.username, u.full_name, u.status, u.enabled " +
        " ORDER BY u.id")) {
        int n = 0;
        while (rs.next()) {
          n++;
          System.out.printf("%4d  %-15s  %-30s  %-7s enabled=%-5s  roles=%s%n",
            rs.getLong(1), rs.getString(2), rs.getString(3),
            rs.getString(4), rs.getBoolean(5), rs.getString(6));
        }
        System.out.println("---- total users: " + n);
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
& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$jar" CheckUsers $url $user $pass
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
