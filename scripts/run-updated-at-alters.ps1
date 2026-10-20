$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$jar = "C:\Users\Khanh\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar"

$url  = if ($env:DB_URL)      { $env:DB_URL }      else { "jdbc:mysql://localhost:3306/hethong_quanlydetai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&useUnicode=true" }
$user = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

$tables = @(
  "departments",
  "users","students","lecturers","topics","registration_periods","student_groups",
  "topic_supervisors","group_members","topic_registrations","reports",
  "councils","council_members","council_topics","review_assignments",
  "evaluations","review_results","notifications","audit_logs"
)

$tmpDir = Join-Path $env:TEMP ("run-alters-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "RunAlters.java"

$source = @'
import java.sql.*;
public class RunAlters {
  public static void main(String[] a) throws Exception {
    String url  = a[0];
    String user = a[1];
    String pass = a[2];
    String[] tables = a[3].split(",");
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection(url, user, pass);
         Statement s = c.createStatement()) {
      for (String t : tables) {
        // Check column existence first (MySQL has no IF NOT EXISTS for ADD COLUMN)
        try (ResultSet chk = s.executeQuery(
          "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
          "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='" + t + "' AND COLUMN_NAME='updated_at'")) {
          chk.next();
          if (chk.getInt(1) > 0) {
            System.out.println("SKIP " + t + " (already has updated_at)");
            continue;
          }
        }
        String ddl = "ALTER TABLE " + t +
          " ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";
        try {
          s.executeUpdate(ddl);
          System.out.println("OK  " + t);
        } catch (SQLException e) {
          System.out.println("ERR " + t + " :: " + e.getMessage());
        }
      }
      System.out.println("---- verify ----");
      try (ResultSet rs = s.executeQuery(
        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
        "WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='updated_at' ORDER BY TABLE_NAME")) {
        while (rs.next()) System.out.println(rs.getString(1));
      }
    }
  }
}
'@

[System.IO.File]::WriteAllText($javaFile, $source, [System.Text.Encoding]::ASCII)
Write-Host "java file: $javaFile ($((Get-Item $javaFile).Length) bytes)"

$prevLoc = (Get-Location).Path
Set-Location $tmpDir
& "$env:JAVA_HOME\bin\javac.exe" $javaFile
if ($LASTEXITCODE -ne 0) { Set-Location $prevLoc; Remove-Item -Recurse $tmpDir; throw "javac failed" }

$tablesArg = $tables -join ","
& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$jar" RunAlters $url $user $pass $tablesArg
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
