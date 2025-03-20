Write-Host "🚀 Build de Trackit Docker..."
docker compose build --no-cache

Write-Host "🐳 Iniciando contenedor Trackit..."
docker compose up -d

Write-Host "✅ Trackit corriendo. Puedes conectar por SSH a puerto 2222."
ssh-keygen -R [localhost]:2222
ssh trackit@localhost -p 2222