# radicle-jetbrains-plugin

![Build](https://github.com/cytechmobile/radicle-jetbrains-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/19664.svg)](https://plugins.jetbrains.com/plugin/19664)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/19664.svg)](https://plugins.jetbrains.com/plugin/19664)

<!-- Plugin description -->
This plugin allows you to start using [Radicle](https://radicle.xyz) (the decentralized Code Collaboration protocol) directly from your Jetbrains IDE.

Radicle enables developers to securely collaborate on software over a peer-to-peer network, built on top of Git,
that provides GitHub/GitLab-like functionality (think Pull/Merge Requests, Issues, etc.).

This plugin requires **version 0.8.0** of the `rad` Command Line Interface (CLI) tool to be installed.
Installation instructions for `rad` are available here: [https://github.com/radicle-dev/heartwood](https://github.com/radicle-dev/heartwood)

This plugin is available under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "radicle-jetbrains-plugin"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/cytechmobile/radicle-jetbrains-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage 

### Prerequisites

* The `rad` Command Line Interface (CLI) tool installed, at least v0.8.0. Please check [here](https://github.com/radicle-dev/heartwood) for installation details. 

  In your terminal, enter the command `rad --version`. The output should be similar to:
```bash
$ rad --version
rad 0.8.0
```
* A configured Radicle identity, with `rad auth`. Check [here](https://github.com/radicle-dev/heartwood/blob/master/radicle-cli/examples/rad-auth.md) for examples.
* A Radicle-initialised Git repo


* On Windows: 
  * Windows Subsystem for Linux (WSL) 2 (The following steps are required in the default WSL2 distribution)
  * Keychain 2.8.5 (e.g. with `sudo apt-get install keychain` or `sudo dnf install keychain` )
  * Add the following line to your bash shell configuration `~/.bashrc`. Even if you are using a different shell, such as zsh, these need to exist in your bash shell configuration.

  ```bash
  eval `keychain --quiet --eval --agents ssh id_rsa`;
  export WSLENV=$WSLENV:SSH_AGENT_PID:SSH_AUTH_SOCK;
  ```
  * Open window terminal and run Intellij from wsl (e.g ```wsl bash -lic "path"``` )
  * Open Intellij and set the ```Git``` executable to be the git installed in Windows Subsystem for linux
  * Run `rad auth` in your project directory


### Accepting Contributed Changes (Pulling from Seed Node)

* Open a Radicle-initialised repo in your Jetbrains IDE.
* Ensure a seed node has been configured. 
* Use `Git -> Radicle -> Fetch` (or the corresponding toolbar icon) to fetch changes from the seed node. 
* Merge changes as appropriate, using your IDE.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
