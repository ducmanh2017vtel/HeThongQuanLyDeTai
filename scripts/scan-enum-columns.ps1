$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$jar = "C:\Users\Khanh\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar"

$url  = if ($env:DB_URL)      { $env:DB_URL }      else { "jdbc:mysql://localhost:3306/hethong_quanlydetai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&useUnicode=true" }
$user = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

$tmpDir = Join-Path $env:TEMP ("scan-enum-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "ScanEnum.java"

$source = @'
import java.sql.*;
public class ScanEnum {
  public static void main(String[] a) throws Exception {
    String url  = a[0];
    String user = a[1];
    String pass = a[2];
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection(url, user, pass);
         Statement s = c.createStatement()) {
      try (ResultSet rs = s.executeQuery(
        "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT " +
        "FROM INFORMATION_SCHEMA.COLUMNS " +
        "WHERE TABLE_SCHEMA=DATABASE() AND DATA_TYPE='enum' " +
        "ORDER BY TABLE_NAME, COLUMN_NAME")) {
        while (rs.next()) {
          String t = rs.getString(1);
          String col = rs.getString(2);
          String colType = rs.getString(3); // e.g. enum('ACTIVE','INACTIVE')
          String nullable = rs.getString(4);
          String def = rs.getString(5);
          // parse enum values
          String inside = colType.substring(colType.indexOf('(') + 1, colType.lastIndexOf(')'));
          String[] vals = inside.split(",");
          int maxLen = 0;
          for (String v : vals) {
            String unq = v.trim();
            if (unq.startsWith("'") && unq.endsWith("'")) unq = unq.substring(1, unq.length()-1);
            if (unq.length() > maxLen) maxLen = unq.length();
          }
          // round up to nearest 5, min 10
          int len = Math.max(10, ((maxLen + 4) / 5) * 5);
          String nullClause = nullable.equals("YES") ? "NULL" : "NOT NULL";
          String defClause = (def == null) ? "" : (" DEFAULT '" + def + "'");
          System.out.println(t + "|" + col + "|VARCHAR|" + len + "|" + nullClause + defClause);
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

& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$jar" ScanEnum $url $user $pass | Tee-Object -Variable spec
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
