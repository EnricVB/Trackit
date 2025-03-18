#!/bin/bash

echo "🚀 Build de Trackit Docker..."
docker compose build

echo "🐳 Iniciando contenedor Trackit..."
docker compose up -d

echo "✅ Trackit corriendo. Puedes conectar por SSH a puerto 2222."
ssh trackit@localhost -p 2222