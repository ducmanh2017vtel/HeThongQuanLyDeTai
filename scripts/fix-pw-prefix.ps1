$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$jar = "C:\Users\Khanh\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar"

$url  = if ($env:DB_URL)      { $env:DB_URL }      else { "jdbc:mysql://localhost:3306/hethong_quanlydetai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&useUnicode=true" }
$user = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

$tmpDir = Join-Path $env:TEMP ("fix-pw-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "FixPw.java"

$source = @'
import java.sql.*;
public class FixPw {
  public static void main(String[] a) throws Exception {
    String url  = a[0];
    String user = a[1];
    String pass = a[2];
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection(url, user, pass);
         Statement s = c.createStatement()) {
      int total = 0, updated = 0, skipped = 0;
      try (ResultSet rs = s.executeQuery("SELECT id, username, password FROM users");
           PreparedStatement upd = c.prepareStatement("UPDATE users SET password=? WHERE id=?")) {
        while (rs.next()) {
          total++;
          long id = rs.getLong(1);
          String username = rs.getString(2);
          String pw = rs.getString(3);
          if (pw == null) { skipped++; continue; }
          // Skip if already has {bcrypt} or other prefix
          if (pw.startsWith("{") && pw.indexOf("}") < 10) {
            skipped++;
            continue;
          }
          String newPw = "{bcrypt}" + pw;
          upd.setString(1, newPw);
          upd.setLong(2, id);
          upd.executeUpdate();
          updated++;
        }
      }
      System.out.println("total=" + total + "  updated=" + updated + "  skipped=" + skipped);
    }
  }
}
'@

[System.IO.File]::WriteAllText($javaFile, $source, [System.Text.Encoding]::ASCII)
$prevLoc = (Get-Location).Path
Set-Location $tmpDir
& "$env:JAVA_HOME\bin\javac.exe" $javaFile
if ($LASTEXITCODE -ne 0) { Set-Location $prevLoc; Remove-Item -Recurse $tmpDir; throw "javac failed" }
& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$jar" FixPw $url $user $pass
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
