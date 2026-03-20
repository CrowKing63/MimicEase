@echo off
chcp 65001 > nul 2>&1
title MimicEase Installer / 설치하기 / インストーラー / 安装程序 / 安裝程式 / Instalador / Installateur / Installationsprogramm / Instalador

powershell.exe -ExecutionPolicy Bypass -NoProfile -File "%~dp0install.ps1"
set PS_EXIT=%errorlevel%

if %PS_EXIT% neq 0 (
  echo.
  echo ================================================================
  echo  ERROR: The installer exited with an error. ^(code: %PS_EXIT%^)
  echo  If you saw a red message above, please check INSTALL_GUIDE.md
  echo  or report the issue at: https://github.com/CrowKing63/MimicEase
  echo ================================================================
  echo.
  pause
)
exit /b %PS_EXIT%
