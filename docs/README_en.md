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

# Trackit - Version Control System

Trackit is an enhanced version control system designed to improve upon existing solutions like Git. This document provides instructions on how to generate and install the `.deb` package for Trackit using `apt`.

## Creating a `.deb` Package for Trackit

Follow these steps to create a `.deb` package for Trackit:

### 1. Create the Package Structure
```bash
    mkdir -p trackit-deb/DEBIAN
    mkdir -p trackit-deb/usr/local/bin
    mkdir -p trackit-deb/usr/share/trackit
```

### 2. Move the `.jar` File and Create an Executable
```bash
    cp path/to/trackit.jar trackit-deb/usr/share/trackit/
    echo '#!/bin/bash
    exec java -jar /usr/share/trackit/trackit.jar "$@"' > trackit-deb/usr/local/bin/trackit
    chmod +x trackit-deb/usr/local/bin/trackit
```

### 3. Create the Control File
Create `trackit-deb/DEBIAN/control` with the following content:
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

### 4. Create the `postinst` Script
```bash
    echo '#!/bin/bash
    chmod +x /usr/local/bin/trackit' > trackit-deb/DEBIAN/postinst
    chmod +x trackit-deb/DEBIAN/postinst
```

### 5. Build the `.deb` Package
```bash
    dpkg-deb --build trackit-deb
```

### 6. Generate `dpkg` Packages Index
```bash
    dpkg-scanpackages -m . > Packages
    gzip -k Packages
```

## Installing Trackit on Ubuntu

To install Trackit via `apt`, follow these steps:

### 1. Add the Repository
```bash
  echo "deb [trusted=yes] https://github.com/EnricVB/Trackit/releases/download/VERSION_TO_DOWNLOAD ./" | tee /etc/apt/sources.list.d/trackit.list
```

### 2. Update the Package List
```bash
    apt update
```

### 3. Install Trackit
```bash
    apt install trackit
```

### 4. Install java
If you don't have Java installed, you can install it using the following commands:

```bash
  apt install zip
    
  curl -s "https://get.sdkman.io" | bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
    
  sdk install java 22-open
```

## Usage
Once installed, you can run Trackit with:
```bash
    trackit --help
```