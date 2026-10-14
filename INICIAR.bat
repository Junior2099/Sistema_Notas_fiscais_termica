@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java nao encontrado. Instale Java 17 ou mais recente e tente novamente.
  pause
  exit /b 1
)
java -jar papelaria.jar
if errorlevel 1 (
  echo Nao foi possivel executar. Verifique se o Java e versao 17 ou mais recente.
  pause
)
