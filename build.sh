#!/usr/bin/env bash
set -e

echo "=== Compiling Task Assignment Backend ==="
mvn clean package -DskipTests

echo "=== Starting Task Assignment Backend on port 8080 ==="
JAR_FILE=$(ls target/*.jar 2>/dev/null | grep -v 'original' | head -n 1)
if [ -n "$JAR_FILE" ]; then
    exec java -jar "$JAR_FILE"
else
    exec mvn spring-boot:run
fi
