@echo off
cd /d "%~dp0"
echo ==========================================
echo Smart Research Paper Recommendation System
echo WEB VERSION
echo ==========================================
echo.
mvn -q clean compile
if errorlevel 1 (echo BUILD FAILED.&pause&exit /b 1)
echo.
echo Open http://localhost:8080 in Chrome.
echo.
java -cp "target\classes" com.itihasiq.research.ResearchWebServer
pause
