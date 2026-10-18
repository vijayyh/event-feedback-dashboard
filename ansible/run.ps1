# Runs an Ansible playbook from Windows on the Ubuntu (WSL) control node and keeps a log of the run.
#
#   .\ansible\run.ps1 provision.yml
#   .\ansible\run.ps1 site.yml -Log docs\evidence\task13-first-run.txt
#   .\ansible\run.ps1 site.yml -Options '-e', 'efd_release_build=13'
#
# The organiser password is read from the git-ignored application-local.properties and handed to WSL in an
# environment variable (WSLENV), so it never appears on a command line or in a log.
param(
    [Parameter(Mandatory = $true)][string]$Playbook,
    [string[]]$Options = @(),
    [string]$Log,
    [string]$Distro = 'Ubuntu-24.04'
)
$repo = Split-Path $PSScriptRoot -Parent
$line = Get-Content (Join-Path $repo 'application-local.properties') -ErrorAction Stop |
    Where-Object { $_ -match '^spring\.security\.user\.password=' } | Select-Object -First 1
if (-not $line) { throw 'spring.security.user.password is missing in application-local.properties' }
$env:EFD_ORGANISER_PASSWORD = $line.Substring($line.IndexOf('=') + 1)
$env:WSLENV = 'EFD_ORGANISER_PASSWORD/u'
# Windows programs started from WSL (docker.exe) inherit this PATH; reload it so tools installed after this
# terminal was opened (Docker Desktop and its credential helpers) are found
$env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('Path', 'User')

$full = (Resolve-Path (Join-Path $PSScriptRoot 'run.sh')).Path
$wslPath = '/mnt/' + $full.Substring(0, 1).ToLower() + ($full.Substring(2) -replace '\\', '/')
try {
    if ($Log) {
        wsl.exe -d $Distro -u root -- bash $wslPath $Playbook @Options 2>&1 | ForEach-Object { "$_" } | Tee-Object -Variable output
        $logFile = Join-Path $repo $Log
        New-Item -ItemType Directory -Force (Split-Path $logFile) | Out-Null
        [IO.File]::WriteAllLines($logFile, [string[]]$output, (New-Object Text.UTF8Encoding $false))
    } else {
        wsl.exe -d $Distro -u root -- bash $wslPath $Playbook @Options
    }
} finally {
    $env:EFD_ORGANISER_PASSWORD = $null
}
