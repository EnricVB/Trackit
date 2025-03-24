# Usa Ubuntu 22.04
FROM ubuntu:22.04

# Instala dependencias necesarias
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    zip \
    bash \
    openssh-server \
    && rm -rf /var/lib/apt/lists/*

# Instala Java 22 directamente
RUN mkdir -p /opt/jdk && \
    curl -L -o /tmp/openjdk-22.tar.gz https://github.com/adoptium/temurin22-binaries/releases/download/jdk-22%2B36/OpenJDK22U-jdk_x64_linux_hotspot_22_36.tar.gz && \
    tar -xzf /tmp/openjdk-22.tar.gz -C /opt/jdk --strip-components=1 && \
    rm /tmp/openjdk-22.tar.gz

# Configura JAVA_HOME y PATH
ENV JAVA_HOME="/opt/jdk"
ENV PATH="$JAVA_HOME/bin:$PATH"

# Asegura que todos los shells lo tengan en PATH
RUN echo 'export JAVA_HOME="/opt/jdk"' >> /etc/profile && \
    echo 'export PATH="/opt/jdk/bin:$PATH"' >> /etc/profile

# Configura usuario y SSH
RUN useradd -ms /bin/bash trackit && \
    echo "trackit:trackit" | chpasswd && \
    mkdir /var/run/sshd && \
    ssh-keygen -A

# Puerto SSH
EXPOSE 22

# Copia el .jar al contenedor
WORKDIR /home/trackit
COPY ./jar/trackit.jar .

WORKDIR /trackit

# Crea script ejecutable: "trackit"
RUN echo -e "#!/bin/bash\njava -jar /home/trackit/trackit.jar \"\$@\"" > /usr/local/bin/trackit && \
    chmod +x /usr/local/bin/trackit


# CMD: iniciar servidor SSH
CMD ["/usr/sbin/sshd", "-D"]
