$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$driveLetter = $env:LIBRIO_E2E_DRIVE
if ([string]::IsNullOrWhiteSpace($driveLetter)) {
    $driveLetter = "X"
}
$drive = "$driveLetter`:"

$maven = $env:MAVEN_CMD
if ([string]::IsNullOrWhiteSpace($maven)) {
    $maven = "C:\Users\Admin\apache-maven-3.9.8\bin\mvn.cmd"
}
if (!(Test-Path $maven)) {
    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($null -eq $mavenCommand) {
        throw "Maven not found. Set MAVEN_CMD to mvn.cmd before running E2E."
    }
    $maven = $mavenCommand.Source
}

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $knownJdk = "C:\Users\Admin\.jdks\ms-17.0.17"
    if (Test-Path $knownJdk) {
        $env:JAVA_HOME = $knownJdk
    }
}

$existing = subst | Select-String "^$([regex]::Escape($drive))\\:"
if ($null -eq $existing) {
    subst $drive "$repoRoot"
}

$env:SPRING_PROFILES_ACTIVE = "e2e"
$env:SERVER_PORT = $env:PLAYWRIGHT_BACKEND_PORT
if ([string]::IsNullOrWhiteSpace($env:SERVER_PORT)) {
    $env:SERVER_PORT = "18080"
}

Set-Location "$drive\backend"
& $maven spring-boot:run "-Dspring-boot.run.profiles=e2e"
