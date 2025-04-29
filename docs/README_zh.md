
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

Trackit 是一个增强的版本控制系统，旨在改进 Git 等现有解决方案。

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

### 4. 安装 Java
如果您没有安装 Java，可以使用以下命令安装：

```bash
  apt install zip
    
  curl -s "https://get.sdkman.io" | bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
    
  sdk install java 22-open
```

## 使用方法
安装完成后，您可以使用以下命令运行 Trackit：
```bash
    trackit --help
```
