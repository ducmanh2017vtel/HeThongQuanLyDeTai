$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$jar = "C:\Users\Khanh\.m2\repository\com\mysql\mysql-connector-j\9.5.0\mysql-connector-j-9.5.0.jar"

$url  = if ($env:DB_URL)      { $env:DB_URL }      else { "jdbc:mysql://localhost:3306/hethong_quanlydetai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&useUnicode=true" }
$user = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$pass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

# (Table, Column, Length, Nullable, Default)
# Default may be $null if no default
$changes = @(
  @{ Table="council_members";        Column="role";         Length=12; Nullable=$false; Default="MEMBER" },
  @{ Table="councils";               Column="status";       Length=10; Nullable=$false; Default="DRAFT" },
  @{ Table="evaluations";            Column="status";       Length=12; Nullable=$false; Default="SUBMITTED" },
  @{ Table="notifications";          Column="status";       Length=10; Nullable=$false; Default="DRAFT" },
  @{ Table="notifications";          Column="type";         Length=12; Nullable=$false; Default="FACULTY" },
  @{ Table="registration_periods";   Column="period_type";  Length=10; Nullable=$false; Default=$null },
  @{ Table="registration_periods";   Column="status";       Length=10; Nullable=$false; Default="DRAFT" },
  @{ Table="reports";                Column="status";       Length=12; Nullable=$false; Default="SUBMITTED" },
  @{ Table="review_assignments";     Column="status";       Length=12; Nullable=$false; Default="ASSIGNED" },
  @{ Table="review_results";         Column="result";       Length=8;  Nullable=$true;  Default=$null },
  @{ Table="student_groups";         Column="status";       Length=12; Nullable=$false; Default="DRAFT" },
  @{ Table="topic_registrations";    Column="status";       Length=10; Nullable=$false; Default="PENDING" },
  @{ Table="topics";                 Column="status";       Length=10; Nullable=$false; Default="PENDING" },
  @{ Table="users";                  Column="status";       Length=10; Nullable=$false; Default="ACTIVE" }
)

$tmpDir = Join-Path $env:TEMP ("conv-enum-" + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$javaFile = Join-Path $tmpDir "ConvEnum.java"

$source = @'
import java.sql.*;
public class ConvEnum {
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
        // table|col|len|nullable(true/false)|default(value or <null>)
        String[] p = row.split("\\|", -1);
        String t = p[0], col = p[1];
        int len = Integer.parseInt(p[2]);
        boolean nullable = Boolean.parseBoolean(p[3]);
        String def = p[4];
        String nullClause = nullable ? "NULL" : "NOT NULL";
        String defClause = (def == null || def.equals("<null>")) ? "" : (" DEFAULT '" + def + "'");
        String ddl = "ALTER TABLE " + t + " MODIFY COLUMN " + col + " VARCHAR(" + len + ") " + nullClause + defClause;
        try {
          s.executeUpdate(ddl);
          System.out.println("OK  " + t + "." + col + " -> VARCHAR(" + len + ") " + nullClause + defClause);
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

$spec = ($changes | ForEach-Object {
  $d = if ($_.Default -eq $null) { "<null>" } else { $_.Default }
  "$($_.Table)|$($_.Column)|$($_.Length)|$($_.Nullable.ToString().ToLower())|$d"
}) -join ";"

& "$env:JAVA_HOME\bin\java.exe" -cp "$tmpDir;$jar" ConvEnum $url $user $pass $spec
Set-Location $prevLoc
Remove-Item -Recurse $tmpDir
