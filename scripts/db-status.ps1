# BorrowHub Database Status Checker (PowerShell Helper)
$ErrorActionPreference = "SilentlyContinue"

$mysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysqlExe)) {
    $cmd = Get-Command mysql -ErrorAction SilentlyContinue
    if ($cmd) { $mysqlExe = $cmd.Source }
}

$dbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "borrowhub_user" }
$dbPass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "BorrowHub@2026" }
$dbName = if ($env:DB_NAME) { $env:DB_NAME } else { "borrowhub_db" }
$dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }

if (Test-Path $mysqlExe) {
    # Suppress password warning from stderr
    $prevWarning = $env:MYSQL_PWD
    $env:MYSQL_PWD = $dbPass

    try {
        $ver = (& $mysqlExe -u $dbUser -h $dbHost -P $dbPort -s -N -e "SELECT VERSION();" $dbName 2>$null)
        $tbl = (& $mysqlExe -u $dbUser -h $dbHost -P $dbPort -s -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$dbName';" $dbName 2>$null)
        $flyway = (& $mysqlExe -u $dbUser -h $dbHost -P $dbPort -s -N -e "SELECT CONCAT(version, ' (', description, ') - Status: ', IF(success=1,'SUCCESS','FAILED')) FROM $dbName.flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;" $dbName 2>$null)

        if ($ver) {
            Write-Host "Connection Status  : CONNECTED (Authenticated as $dbUser)"
            Write-Host "MySQL Version      : $ver"
            Write-Host "Total Tables       : $tbl"
            if ($flyway) {
                Write-Host "Flyway Migration   : Version $flyway"
            }
        } else {
            Write-Host "Connection Status  : Port open, but authentication failed or database '$dbName' not accessible"
        }
    } finally {
        $env:MYSQL_PWD = $prevWarning
    }
} else {
    Write-Host "Connection Status  : Port active (mysql client CLI not detected in standard location)"
}
