#!/bin/bash

# Comprobar si se ejecuta como root
if [ "$EUID" -ne 0 ]; then
  echo "Este script debe ejecutarse como root (sudo)"
  exit 1
fi

# Añadir el repositorio
echo "deb [trusted=yes] https://github.com/EnricVB/Trackit/releases/download/latest/ ./" > /etc/apt/sources.list.d/trackit.list

# Actualizar índices de apt
apt update

# Instalar Trackit
apt install -y trackit

echo "Trackit ha sido instalado correctamente."