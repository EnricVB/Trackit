#!/bin/bash

# Este script genera los archivos Packages y Packages.gz necesarios para un repositorio APT
# Debe ejecutarse en el directorio donde está el archivo .deb

# Verificar que existe un archivo .deb
if [ ! -f *.deb ]; then
  echo "No se encontró ningún archivo .deb en el directorio actual."
  exit 1
fi

# Crear el archivo Packages
dpkg-scanpackages -m . > Packages
gzip -k -f Packages

echo "Archivos de repositorio generados correctamente."
echo "Sube los archivos .deb, Packages y Packages.gz a tu GitHub Release."
