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

Trackit es un sistema de control de versiones diseñado para mejorar las soluciones existentes como Git.

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
