@echo off
chcp 65001 > nul
title MimicEase Installer / 설치하기 / インストーラー / 安装程序 / 安裝程式 / Instalador / Installateur / Installationsprogramm / Instalador
powershell.exe -ExecutionPolicy Bypass -NoProfile -File "%~dp0install.ps1"
exit /b %errorlevel%
