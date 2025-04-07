
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

# Trackit - 版本控制系统

Trackit 是一个增强的版本控制系统，旨在改进 Git 等现有解决方案。本文档提供了使用 `apt` 生成和安装 Trackit 的 `.deb` 软件包的说明。

## 创建 Trackit 的 `.deb` 软件包

按照以下步骤创建 Trackit 的 `.deb` 软件包：

### 1. 创建软件包结构
```bash
    mkdir -p trackit-deb/DEBIAN
    mkdir -p trackit-deb/usr/local/bin
    mkdir -p trackit-deb/usr/share/trackit
```

### 2. 移动 `.jar` 文件并创建可执行文件
```bash
    cp path/to/trackit.jar trackit-deb/usr/share/trackit/
    echo '#!/bin/bash
    exec java -jar /usr/share/trackit/trackit.jar "$@"' > trackit-deb/usr/local/bin/trackit
    chmod +x trackit-deb/usr/local/bin/trackit
```

### 3. 创建控制文件
创建 `trackit-deb/DEBIAN/control` 文件，内容如下：
```
Package: trackit
Version: 1.0
Section: utils
Priority: optional
Architecture: all
Depends: default-jre
Maintainer: Enric Velasco <enricvbufi@gmail.com>
Description: Trackit - 增强的版本控制系统
```

### 4. 创建 `postinst` 脚本
```bash
    echo '#!/bin/bash
    chmod +x /usr/local/bin/trackit' > trackit-deb/DEBIAN/postinst
    chmod +x trackit-deb/DEBIAN/postinst
```

### 5. 构建 `.deb` 软件包
```bash
    dpkg-deb --build trackit-deb
```

### 6. 生成 `dpkg` 软件包索引
```bash
    dpkg-scanpackages -m . > Packages
    gzip -k Packages
```

## 在 Ubuntu 上安装 Trackit

要通过 `apt` 安装 Trackit，请执行以下步骤：

### 1. 添加软件源
```bash
  echo "deb [trusted=yes] https://github.com/EnricVB/Trackit/releases/download/VERSION_TO_DOWNLOAD ./" | tee /etc/apt/sources.list.d/trackit.list
```

### 2. 更新软件包列表
```bash
    apt update
```

### 3. 安装 Trackit
```bash
    apt install trackit
```

## 使用方法
安装完成后，您可以使用以下命令运行 Trackit：
```bash
    trackit --help
```
