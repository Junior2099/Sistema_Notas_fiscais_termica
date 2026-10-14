@echo off
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Para compilar, instale um JDK 17 ou mais recente com javac e jar no PATH.
  pause
  exit /b 1
)
if not exist build\classes mkdir build\classes
javac --release 17 -encoding UTF-8 -d build\classes src\br\com\papelaria\Ui.java src\br\com\papelaria\App.java src\br\com\papelaria\SelfTest.java src\br\com\papelaria\UiCheck.java src\br\com\papelaria\ReceiptPdf.java src\br\com\papelaria\Platform.java src\br\com\papelaria\PdfPreview.java
if errorlevel 1 goto falha
jar --create --file papelaria.jar --main-class br.com.papelaria.App -C build\classes .
if errorlevel 1 goto falha
echo Compilado com sucesso.
pause
exit /b 0
:falha
echo Falha na compilacao. Confira o erro acima.
pause
exit /b 1
