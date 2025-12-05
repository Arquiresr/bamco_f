<#
PowerShell script to initialize Git repo, add remote, commit and push to GitHub.
This script assumes Git is installed locally and that you will provide credentials when prompted.

Usage:
  cd /d D:\Mamute
  powershell -ExecutionPolicy Bypass -File .\publish_to_github.ps1

The script will perform a forced push to `origin` -> `main`. Change `--force` to `--force-with-lease` if you prefer.
#>

# ensure running from script directory
Set-Location -Path $PSScriptRoot

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Error "git não encontrado no PATH. Instale Git (https://git-scm.com/download/win) e execute novamente."
    exit 1
}

if (-not (Test-Path .git)) {
    git init
    git checkout -b main
} else {
    git rev-parse --is-inside-work-tree 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Diretório atual não parece ser um repositório Git válido."
        exit 1
    }
    # try to checkout main or create it
    git checkout main 2>$null
    if ($LASTEXITCODE -ne 0) { git checkout -b main }
}

# remove existing origin to avoid duplicate errors
git remote remove origin 2>$null
git remote add origin https://github.com/Arquiresr/bamco_f

git add .
# try a normal commit, if nothing to commit, create an empty commit so repo has a commit
git commit -m "Initial commit from local workspace" 2>$null
if ($LASTEXITCODE -ne 0) {
    git commit --allow-empty -m "Initial commit from local workspace"
}

Write-Host "Fazendo push forçado para origin/main..." -ForegroundColor Yellow
git push origin main --force

if ($LASTEXITCODE -eq 0) {
    Write-Host "Push concluído com sucesso." -ForegroundColor Green
} else {
    Write-Error "Falha no push. Revise mensagens de erro acima e tente novamente." 
    exit 1
}
