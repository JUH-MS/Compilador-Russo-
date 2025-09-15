@echo off
REM Compilar o arquivo .jj
javacc src\Compilador.jj

REM Compilar os arquivos .java gerados
javac src\*.java

echo Compilação concluída!
pause
