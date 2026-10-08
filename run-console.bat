@echo off
cd /d "%~dp0"
mvn -q -DskipTests compile
java -cp "target/classes" com.itihasiq.research.Main
pause
