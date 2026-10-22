@echo off
rem =====================================================================
rem GestiMed : sauvegarde complète de la base « medicale » avec mysqldump.
rem
rem À préférer à l'export de phpMyAdmin, qui perd le moteur, le jeu de
rem caractères et les contraintes CHECK des tables patient, consultation,
rem prescrire et demandeanalyse.
rem
rem Usage : sql\sauvegarder_base.bat [dossier] [jours]
rem   dossier : où écrire (D:\AutodevSoftware par défaut ; un disque externe
rem             de préférence) ;
rem   jours   : les sauvegardes plus anciennes sont supprimées (30 par défaut,
rem             0 pour tout garder).
rem
rem Avec un fichier sauvegarde.cnf à côté de ce script (voir
rem sql\creer_compte_sauvegarde.sql), aucun mot de passe n'est demandé : la
rem sauvegarde peut être planifiée chaque jour. Sans lui, le mot de passe de
rem root est demandé.
rem
rem Restauration : D:\xampp\mysql\bin\mysql.exe -u root -p medicale < fichier.sql
rem =====================================================================
setlocal
set MYSQLDUMP=D:\xampp\mysql\bin\mysqldump.exe
set DOSSIER=%~1
if "%DOSSIER%"=="" set DOSSIER=D:\AutodevSoftware
set JOURS=%~2
if "%JOURS%"=="" set JOURS=30
if not exist "%DOSSIER%" mkdir "%DOSSIER%"
for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HHmm"') do set HORODATAGE=%%d
set FICHIER=%DOSSIER%\sauvegarde_medicale_%HORODATAGE%.sql

set IDENTIFIANTS=-u root -p
if exist "%~dp0sauvegarde.cnf" set IDENTIFIANTS=--defaults-extra-file="%~dp0sauvegarde.cnf"

"%MYSQLDUMP%" %IDENTIFIANTS% --single-transaction --routines --triggers --events --default-character-set=utf8mb4 --result-file="%FICHIER%" medicale
if errorlevel 1 (
  echo Echec de la sauvegarde.
  if exist "%FICHIER%" del "%FICHIER%"
  exit /b 1
)
echo Sauvegarde ecrite dans %FICHIER%

if not "%JOURS%"=="0" (
  forfiles /p "%DOSSIER%" /m sauvegarde_medicale_*.sql /d -%JOURS% /c "cmd /c del @path" 2>nul
)
exit /b 0
