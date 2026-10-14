@echo off
cd /d "%~dp0"
java -Djava.awt.headless=true -cp papelaria.jar br.com.papelaria.SelfTest
if errorlevel 1 goto falha
java -Djava.awt.headless=true -cp papelaria.jar br.com.papelaria.UiCheck
if errorlevel 1 goto falha
pause
exit /b 0
:falha
echo Um teste falhou. Confira o erro acima.
pause
exit /b 1
