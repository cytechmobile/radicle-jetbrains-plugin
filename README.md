# radicle-jetbrains-plugin

![Build](https://github.com/cytechmobile/radicle-jetbrains-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/19664.svg)](https://plugins.jetbrains.com/plugin/19664)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/19664.svg)](https://plugins.jetbrains.com/plugin/19664)

<!-- Plugin description -->
Integration of [Radicle](https://radicle.network) with Jetbrains IDEs.

Radicle enables developers to securely collaborate on software over a peer-to-peer network built on Git.

This plugin provides integration with Radicle directly within Jetbrains IDEs. 

It requires the Git plugin, as well as [radicle cli](https://radicle.network/get-started.html) to be installed.
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

* The `rad` Command Line Interface (CLI) tool installed, at least v0.6.0. Please check [here](https://radicle.xyz/get-started) for installation details. 

  In your terminal, enter the command `rad --version`. The output should be similar to:
```bash
$ rad --version
rad 0.6.0
```
* A configured Radicle identity, with `rad auth`. Follow the [Radicle CLI quick start guide](https://radicle.network/get-started.html) for more info.
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
  
### Contributing Changes (Pushing to Seed Node)

* Open a Radicle-initialised repo in your Jetbrains IDE.
* Make some changes and commit them locally.
* You now need to use `Git -> Radicle -> Push` from the main menu, which will: 
  * push these changes to your local Radicle monorepo (happens with `git push rad`), 
  * sync these changes with the seed node (happens with `rad sync`).


### Accepting Contributed Changes (Pulling from Seed Node)

* Open a Radicle-initialised repo in your Jetbrains IDE.
* Ensure a seed node has been configured. 
* Use `Git -> Radicle -> Pull` (or the corresponding toolbar icon) to fetch changes from the seed node. 
* Merge changes as appropriate, using your IDE.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
