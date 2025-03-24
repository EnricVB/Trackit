#!/bin/bash

echo "🚀 Build de Trackit Docker..."
docker compose build --no-cache

echo "🐳 Iniciando contenedor Trackit..."
docker compose up -d