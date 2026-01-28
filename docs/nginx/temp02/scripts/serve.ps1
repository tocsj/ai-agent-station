Set-Location (Join-Path $PSScriptRoot "..\dist")

python -m http.server 8080
