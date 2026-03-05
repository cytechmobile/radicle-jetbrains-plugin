vm_name = File.basename(Dir.getwd)

Vagrant.configure("2") do |config|
  config.vm.box = "bento/ubuntu-24.04"

  # Forward port for Remote Robot UI testing
  config.vm.network "forwarded_port", guest: 8082, host: 8082, auto_correct: true

  config.vm.synced_folder ".", "/vagrant", type: "virtualbox"

  config.vm.provider "virtualbox" do |vb|
    vb.memory = "8192"  # Increased for IntelliJ development
    vb.cpus = 4         # Increased for parallel builds
    vb.gui = false
    vb.name = vm_name
    vb.customize ["modifyvm", :id, "--audio", "none"]
    vb.customize ["modifyvm", :id, "--usb", "off"]
  end

  config.vm.provision "shell", inline: <<-SHELL
    export DEBIAN_FRONTEND=noninteractive

    echo "=== Updating system packages ==="
    apt-get update
    apt-get upgrade -y

    echo "=== Installing build essentials and tools ==="
    apt-get install -y \
      build-essential \
      curl \
      wget \
      git \
      unzip \
      jq \
      xvfb \
      x11-utils \
      libxtst6 \
      libxrender1 \
      libfontconfig1 \
      libxi6 \
      libgconf-2-4 \
      make

    echo "=== Installing Java 21 ==="
    apt-get install -y openjdk-21-jdk
    update-alternatives --set java /usr/lib/jvm/java-21-openjdk-arm64/bin/java

    echo "=== Installing Rust ==="
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
    source /root/.cargo/env

    # Add x86_64 Linux target for jrad builds
    rustup target add x86_64-unknown-linux-gnu

    echo "=== Installing Radicle CLI ==="
    # Install rad CLI tool using official installation script
    RADICLE_VERSION="1.6.1"
    curl -sSf https://radicle.xyz/install | sh -s -- --version "\${RADICLE_VERSION}"

    echo "=== Installing Node.js and Claude CLI ==="
    curl -fsSL https://claude.ai/install.sh | bash

    echo "=== Setting up vagrant user environment ==="
    # Make Rust available to vagrant user
    sudo -u vagrant bash -c 'curl --proto "=https" --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y'
    sudo -u vagrant bash -c 'source /home/vagrant/.cargo/env && rustup target add x86_64-unknown-linux-gnu'

    # Set JAVA_HOME for vagrant user (auto-detect arch so it works on both amd64 and arm64)
    JAVA_HOME_DIR=$(dirname $(dirname $(readlink -f /usr/bin/java)))
    echo "export JAVA_HOME=${JAVA_HOME_DIR}" >> /home/vagrant/.bashrc
    echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /home/vagrant/.bashrc

    # Add Rust to PATH for vagrant user
    echo 'source $HOME/.cargo/env' >> /home/vagrant/.bashrc

    # Set DISPLAY for UI tests
    echo 'export DISPLAY=:99' >> /home/vagrant/.bashrc

    # Set path for XDG bin
    echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc && source ~/.bashrc

    # Set project permissions
    chown -R vagrant:vagrant /vagrant

    echo "=== Starting Xvfb for UI tests ==="
    # Start Xvfb for headless UI testing
    Xvfb :99 -screen 0 1920x1080x24 > /dev/null 2>&1 &

    # Create systemd service for Xvfb
    cat > /etc/systemd/system/xvfb.service <<'EOF'
[Unit]
Description=X Virtual Frame Buffer Service
After=network.target

[Service]
ExecStart=/usr/bin/Xvfb :99 -screen 0 1920x1080x24
Restart=always

[Install]
WantedBy=multi-user.target
EOF

    systemctl enable xvfb
    systemctl start xvfb

    echo "=== Verifying installations ==="
    java -version
    /usr/local/bin/rad --version || echo "Radicle CLI not available yet (expected on first install)"
    rustc --version
    node --version
    npm --version
    claude --version

    echo "=== Setup complete! ==="
    echo ""
    echo "To get started:"
    echo "  1. vagrant ssh"
    echo "  2. cd /vagrant"
    echo "  3. ./gradlew build"
    echo "  4. ./gradlew test"
    echo ""
    echo "Available commands:"
    echo "  - ./gradlew build          # Build the plugin"
    echo "  - ./gradlew test           # Run tests (excludes UI tests)"
    echo "  - ./gradlew runIde         # Run plugin in development IDE"
    echo "  - ./gradlew uiTest         # Run UI tests (requires runIdeForUiTests)"
    echo "  - claude                   # Start Claude CLI"

  SHELL
end
