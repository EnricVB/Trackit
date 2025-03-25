#!/bin/bash

echo "🚀 Build de Trackit Docker..."
docker compose build --no-cache

echo "🐳 Iniciando contenedor Trackit..."
docker compose up -d

Write-Host "✅ Trackit corriendo. Conectandose a la consola..."
docker exec -u 0 -it trackit-container bash