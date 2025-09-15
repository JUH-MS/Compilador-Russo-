@echo off
REM ----------------------------------------
REM JavaCC 6.1.2 - Compilação de gramática .jj
REM ----------------------------------------

REM Pasta de origem dos arquivos .jj
set SRC_DIR=src

REM Pasta de destino para arquivos .class
set BIN_DIR=bin

REM Caminho do JavaCC
set JAVACC_JAR=lib\javacc.jar

REM Criar a pasta bin se não existir
if not exist %BIN_DIR% mkdir %BIN_DIR%

REM Gerar arquivos .java a partir do .jj
java -cp %JAVACC_JAR% javacc -OUTPUT_DIRECTORY=%SRC_DIR% %SRC_DIR%\Compilador.jj

REM Compilar os arquivos .java gerados para a pasta bin
javac -d %BIN_DIR% %SRC_DIR%\*.java

echo Compilação concluída!
pause
