
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

# Trackit - Versionskontrollsystem

Trackit ist ein erweitertes Versionskontrollsystem, das bestehende Lösungen wie Git verbessern soll. Dieses Dokument enthält Anweisungen zur Erstellung und Installation des `.deb`-Pakets für Trackit mit `apt`.

## Erstellung eines `.deb`-Pakets für Trackit

Befolgen Sie diese Schritte, um ein `.deb`-Paket für Trackit zu erstellen:

### 1. Paketstruktur erstellen
```bash
    mkdir -p trackit-deb/DEBIAN
    mkdir -p trackit-deb/usr/local/bin
    mkdir -p trackit-deb/usr/share/trackit
```

### 2. Die `.jar`-Datei verschieben und eine ausführbare Datei erstellen
```bash
    cp pfad/zu/trackit.jar trackit-deb/usr/share/trackit/
    echo '#!/bin/bash
    exec java -jar /usr/share/trackit/trackit.jar "$@"' > trackit-deb/usr/local/bin/trackit
    chmod +x trackit-deb/usr/local/bin/trackit
```

### 3. Die Steuerdatei erstellen
Erstellen Sie `trackit-deb/DEBIAN/control` mit folgendem Inhalt:
```
Package: trackit
Version: 1.0
Section: utils
Priority: optional
Architecture: all
Depends: default-jre
Maintainer: Enric Velasco <enricvbufi@gmail.com>
Description: Trackit - Erweitertes Versionskontrollsystem
```

### 4. Das `postinst`-Skript erstellen
```bash
    echo '#!/bin/bash
    chmod +x /usr/local/bin/trackit' > trackit-deb/DEBIAN/postinst
    chmod +x trackit-deb/DEBIAN/postinst
```

### 5. Das `.deb`-Paket bauen
```bash
    dpkg-deb --build trackit-deb
```

### 6. `dpkg`-Paketindex generieren
```bash
    dpkg-scanpackages -m . > Packages
    gzip -k Packages
```

## Installation von Trackit unter Ubuntu

Um Trackit über `apt` zu installieren, führen Sie folgende Schritte aus:

### 1. Repository hinzufügen
```bash
  echo "deb [trusted=yes] https://github.com/EnricVB/Trackit/releases/download/VERSION_TO_DOWNLOAD ./" | tee /etc/apt/sources.list.d/trackit.list
```

### 2. Paketliste aktualisieren
```bash
    apt update
```

### 3. Trackit installieren
```bash
    apt install trackit
```

## Verwendung
Nach der Installation können Sie Trackit mit folgendem Befehl ausführen:
```bash
    trackit --help
```
