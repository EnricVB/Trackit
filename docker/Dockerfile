# Instala Ubuntu 22.04 base
FROM ubuntu:22.04

# Actualizar paquetes e instalar JDK, curl, unzip, OpenSSH
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    zip \
    openssh-server \
    bash \
    && rm -rf /var/lib/apt/lists/*

# Instalar SDKMAN (en root) y usarlo para instalar Java 22 y Kotlin
RUN curl -s "https://get.sdkman.io" | bash && \
bash -c "source /root/.sdkman/bin/sdkman-init.sh && sdk install java 22.3.r17-nik && sdk install kotlin"

# Instalar Kotlin directamente (sin SDKMAN, más fiable en Docker)
RUN curl -L -o kotlin.zip https://github.com/JetBrains/kotlin/releases/download/v1.9.10/kotlin-compiler-1.9.10.zip && \
    unzip kotlin.zip -d /opt/kotlin && \
    rm kotlin.zip

# Agregar Kotlin al PATH
ENV PATH="/opt/kotlin/kotlinc/bin:${PATH}"

# Configurar usuario para SSH
RUN useradd -ms /bin/bash trackit && \
    echo "trackit:trackit" | chpasswd

# Crear directorio para SSH y host keys
RUN mkdir /var/run/sshd && \
    ssh-keygen -A

# Exponer puerto SSH
EXPOSE 22

# Copiar archivos como root, cambiar permisos
WORKDIR /home/app/trackit
COPY ./build/libs/trackit.jar /home/app/trackit
RUN chmod +x /home/app/trackit/trackit.jar


# Usar CMD con JSON (Docker best practice) - lanza SSH server
CMD ["/usr/sbin/sshd", "-D"]
