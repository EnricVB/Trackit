<p align="center" style="display: flex; justify-content: space-around; gap: 10px;">
  <a href="https://github.com/EnricVB/Trackit/tree/master/docs/README_en.md">
    <img src="https://img.shields.io/badge/lang-en-red.svg" alt="English">
  </a>
  <a href="https://github.com/EnricVB/Trackit/tree/master/docs/README_es.md">
    <img src="https://img.shields.io/badge/lang-es-yellow.svg" alt="Español">
  </a>
  <a href="https://github.com/EnricVB/Trackit/tree/master/docs/README_de.md">
    <img src="https://img.shields.io/badge/lang-de-blue.svg" alt="Deutsch">
  </a>
  <a href="https://github.com/EnricVB/Trackit/tree/master/docs/README_zh.md">
    <img src="https://img.shields.io/badge/lang-zh--cn-orange.svg" alt="中文">
  </a>
</p>


# Trackit - Sistema de Control de Versiones

Trackit es un sistema de control de versiones diseñado para mejorar las soluciones existentes como Git. Este documento
proporciona instrucciones sobre cómo generar e instalar el paquete `.deb` para Trackit utilizando `apt`.

## Creando un paquete `.deb` para Trackit

Sigue estos pasos para crear un paquete `.deb` para Trackit:

### 1. Crear la Estructura del Paquete

```bash
  mkdir -p trackit-deb/DEBIAN
  mkdir -p trackit-deb/usr/local/bin
  mkdir -p trackit-deb/usr/share/trackit
```

### 2. Mover el Archivo `.jar` y Crear un Ejecutable

```bash
  cp path/to/trackit.jar trackit-deb/usr/share/trackit/
  echo '#!/bin/bash
  exec java -jar /usr/share/trackit/trackit.jar "$@"' > trackit-deb/usr/local/bin/trackit
  chmod +x trackit-deb/usr/local/bin/trackit
```

### 3. Crear el Archivo de Control

Crea `trackit-deb/DEBIAN/control` con el siguiente contenido:

```
Package: trackit
Version: 1.0
Section: utils
Priority: optional
Architecture: all
Depends: default-jre
Maintainer: Enric Velasco <enricvbufi@gmail.com>
Description: Trackit - Enhanced Version Control System
```

### 4. Crear el Script `postinst`

```bash
  echo '#!/bin/bash
  chmod +x /usr/local/bin/trackit' > trackit-deb/DEBIAN/postinst
  chmod +x trackit-deb/DEBIAN/postinst
```

### 5. Construir el Paquete `.deb`

```bash
  dpkg-deb --build trackit-deb
```

### 6. Generar el Índice de Paquetes `dpkg`

```bash
  dpkg-scanpackages -m . > Packages
  gzip -k Packages
```

## Instalando Trackit en Ubuntu

Para instalar Trackit en Ubuntu, puedes usar el paquete `.deb` que has creado. Asegúrate de tener `dpkg` y `apt`
instalados en tu sistema.

### 1. Agregar el Repositorio

```bash
  echo "deb [trusted=yes] https://github.com/EnricVB/Trackit/releases/download/VERSION_TO_DOWNLOAD ./" | tee /etc/apt/sources.list.d/trackit.list
```

### 2. Actualizar la Lista de Paquetes

```bash
    apt update
```

### 3. Instalar Trackit

```bash
    apt install trackit
```

### 4. Instalar JDK +22
Si no tienes Java instalado, puedes instalarlo con SDKMAN:

```bash
  apt install zip
    
  curl -s "https://get.sdkman.io" | bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
    
  sdk install java 22-open
```

## Uso

Una vez instalado, puedes ejecutar Trackit con:

```bash
    trackit --help
```