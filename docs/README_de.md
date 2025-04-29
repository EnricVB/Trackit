
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

Trackit ist ein erweitertes Versionskontrollsystem, das bestehende Lösungen wie Git verbessern soll. 

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

### 4. Java installieren
Wenn du nicht hast Java installiert, kannst du installieren mit:
```bash
  apt install zip
    
  curl -s "https://get.sdkman.io" | bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
    
  sdk install java 22-open
```

## Verwendung
Nach der Installation können Sie Trackit mit folgendem Befehl ausführen:
```bash
    trackit --help
```
