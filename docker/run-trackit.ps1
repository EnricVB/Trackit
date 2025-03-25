Write-Host "🚀 Build de Trackit Docker..."
wsl docker compose build --no-cache

Write-Host "🐳 Iniciando contenedor Trackit..."
wsl docker compose up -d

Write-Host "✅ Trackit corriendo. Conectandose a la consola..."
wsl docker exec -u 0 -it trackit-container bash