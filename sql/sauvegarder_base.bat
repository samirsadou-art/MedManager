@echo off
rem =====================================================================
rem GestiMed : sauvegarde complète de la base « medicale » avec mysqldump.
rem
rem À préférer à l'export de phpMyAdmin, qui perd le moteur, le jeu de
rem caractères et les contraintes CHECK des tables patient, consultation,
rem prescrire et demandeanalyse.
rem
rem Usage : sql\sauvegarder_base.bat [dossier]   (D:\AutodevSoftware par défaut)
rem Le mot de passe de root est demandé (laisser vide s'il n'y en a pas).
rem Restauration : D:\xampp\mysql\bin\mysql.exe -u root -p medicale < fichier.sql
rem =====================================================================
setlocal
set MYSQLDUMP=D:\xampp\mysql\bin\mysqldump.exe
set DOSSIER=%~1
if "%DOSSIER%"=="" set DOSSIER=D:\AutodevSoftware
for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HHmm"') do set HORODATAGE=%%d
set FICHIER=%DOSSIER%\sauvegarde_medicale_%HORODATAGE%.sql

"%MYSQLDUMP%" -u root -p --single-transaction --routines --triggers --default-character-set=utf8mb4 --result-file="%FICHIER%" medicale
if errorlevel 1 (
  echo Echec de la sauvegarde.
  exit /b 1
)
echo Sauvegarde ecrite dans %FICHIER%
