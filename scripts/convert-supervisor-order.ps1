$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$jar = "C:\Users\Khanh\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar"

$url  = if ($env:DB_URL)      { $env:DB_URL }      else { "jdbc:mysql://localhost:3306/hethong_quanlydetai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&useUnicode=true" }
$user = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

# Convert tinyint to INT for non-boolean columns
$changes = @(
  @{ Table="topic_supervisors"; Column="supervisor_order"; NewType="INT" }
)

$tmpDir = Join-Path $env:TEMP ("conv-int-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "ConvInt.java"

$source = @'
import java.sql.*;
public class ConvInt {
  public static void main(String[] a) throws Exception {
    String url  = a[0];
    String user = a[1];
    String pass = a[2];
    String spec = a[3];
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection(url, user, pass);
         Statement s = c.createStatement()) {
      for (String row : spec.split(";")) {
        if (row.isBlank()) continue;
        String[] p = row.split("\\|");
        String t = p[0], col = p[1], newType = p[2];
        String ddl = "ALTER TABLE " + t + " MODIFY COLUMN " + col + " " + newType + " NOT NULL";
        try {
          s.executeUpdate(ddl);
          System.out.println("OK  " + t + "." + col + " -> " + newType);
        } catch (SQLException e) {
          System.out.println("ERR " + t + "." + col + " :: " + e.getMessage());
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

$spec = ($changes | ForEach-Object { "$($_.Table)|$($_.Column)|$($_.NewType)" }) -join ";"
& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$jar" ConvInt $url $user $pass $spec
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
