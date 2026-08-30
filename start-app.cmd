@echo off
cd /d %~dp0
set JAVA_HOME=C:\Program Files\Java\jdk-25.0.3
call mvnw.cmd spring-boot:run > run.log 2>&1
