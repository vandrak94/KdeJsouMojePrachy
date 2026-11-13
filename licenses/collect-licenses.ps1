param(
    [Parameter(Mandatory=$true)][string]$AppDir,
    [Parameter(Mandatory=$true)][string]$JarDir,
    [Parameter(Mandatory=$true)][string]$ProjectLicenses
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$destination = Join-Path $AppDir 'licenses'
New-Item -ItemType Directory -Path $destination -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $ProjectLicenses 'THIRD-PARTY-NOTICES.txt') -Destination $destination -Force
Copy-Item -LiteralPath (Join-Path $ProjectLicenses 'README.txt') -Destination $destination -Force

# Copy complete license/notice documents embedded in every bundled runtime JAR.
# Do not infer license from artifact name: keep exactly what each JAR distributes.
$jarFiles = @(Get-ChildItem -LiteralPath $JarDir -Filter '*.jar' -File)
if ($jarFiles.Count -eq 0) { throw 'No runtime JARs found for license collection.' }
$manifest = New-Object System.Collections.Generic.List[string]
foreach ($jar in $jarFiles) {
    $jarDestination = Join-Path $destination ('java-libraries\' + $jar.BaseName)
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
    $found = 0
    try {
        foreach ($entry in $zip.Entries) {
            $entryName = $entry.FullName.Replace('\', '/')
            # License texts commonly live in META-INF or a top-level licenses folder.
            if ($entryName -match '(?i)(^|/)(LICENSE|LICENCE|NOTICE|COPYING|COPYRIGHT|THIRD[-_]?PARTY[-_]?LICENSES?)([._/-]|$)' -and
                $entryName -notmatch '/$' -and $entry.Length -gt 0) {
                $safeName = $entryName -replace '[^a-zA-Z0-9._/-]', '_'
                $target = Join-Path $jarDestination ($safeName.Replace('/', '\'))
                $parent = Split-Path -Parent $target
                New-Item -ItemType Directory -Path $parent -Force | Out-Null
                [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
                $found++
            }
        }
    } finally { $zip.Dispose() }
    $manifest.Add(('{0}: {1} license/notice file(s)' -f $jar.Name, $found))
    if ($found -eq 0) {
        Write-Warning ("No embedded license/notice found in {0}; verify this dependency manually before distributing." -f $jar.Name)
    }
}
$manifest | Set-Content -LiteralPath (Join-Path $destination 'BUNDLED-JARS.txt') -Encoding UTF8

# jpackage's reduced runtime normally retains legal/. Include all of its notices.
$runtimeLegal = Join-Path $AppDir 'runtime\legal'
if (Test-Path -LiteralPath $runtimeLegal) {
    Copy-Item -LiteralPath $runtimeLegal -Destination (Join-Path $destination 'openjdk-legal') -Recurse -Force
} else {
    # On some JDK distributions, jpackage may omit legal/ from the runtime image.
    # Copy from the build JDK instead, and make the omission visible.
    $jdkLegal = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'legal' } else { $null }
    if ($jdkLegal -and (Test-Path -LiteralPath $jdkLegal)) {
        Copy-Item -LiteralPath $jdkLegal -Destination (Join-Path $destination 'openjdk-legal') -Recurse -Force
        Write-Warning 'Runtime legal/ was missing; copied license files from JAVA_HOME instead.'
    } else {
        throw 'No JDK legal/ folder found in runtime or JAVA_HOME. Check JDK redistribution terms.'
    }
}
Write-Host ('License notices bundled at: ' + $destination)
