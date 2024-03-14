# radicle-jetbrains-plugin

![Build](https://github.com/cytechmobile/radicle-jetbrains-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/19664.svg)](https://plugins.jetbrains.com/plugin/19664)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/19664.svg)](https://plugins.jetbrains.com/plugin/19664)


<!-- Plugin description -->

This plugin allows you to start using [Radicle](https://radicle.xyz) (the decentralized Code Collaboration protocol) directly from your Jetbrains IDE.

Radicle is an open source, peer-to-peer code collaboration stack. It leverages Git’s architecture combined with cryptography and a gossip protocol to enable a fully sovereign developer stack.

Radicle enables developers to securely collaborate on software over a peer-to-peer network, built on top of Git,
that provides GitHub/GitLab-like functionality (think Pull/Merge Requests, Issues, etc.).

This plugin requires the `rad` Command Line Interface (CLI) tool to be installed.
Installation instructions for `rad` are available here: [https://docs.radicle.xyz/guides/user](https://docs.radicle.xyz/guides/user)

This plugin is available under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
<!-- Plugin description end -->

---

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "radicle-jetbrains-plugin"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/cytechmobile/radicle-jetbrains-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---

## Usage 

### Prerequisites

* The `rad` Command Line Interface (CLI) tool installed. Please check [here](https://docs.radicle.xyz/guides/user) for installation details. 

  In your terminal, enter the command `rad --version`. The output should be similar to:
```bash
$ rad --version
rad 1.0.0
```
* A configured Radicle identity, with `rad auth`. Check [here](https://docs.radicle.xyz/guides/user#come-into-being-from-the-elliptic-aether) for examples.

* A Radicle-initialised Git repo

* In order to be able to sync changes, your radicle-node must be running. Please check [here](https://docs.radicle.xyz/guides/user#operate-nodes-smoothly) for instructions on operating your radicle node.

* In order to be able to browse projects, issues and patches, your radicle-httpd must be running. In order to start it, you can enter the following command in your terminal: `radicle-httpd`. 

* On Windows: 
  * Windows Subsystem for Linux (WSL) 2 (The following steps are required in the default WSL2 distribution)
  * Keychain 2.8.5 (e.g. with `sudo apt-get install keychain` or `sudo dnf install keychain` )
  * Add the following line to your bash shell configuration `~/.bashrc`. Even if you are using a different shell, such as zsh, these need to exist in your bash shell configuration.

  ```bash
  eval `keychain --quiet --eval --agents ssh id_rsa`;
  export WSLENV=$WSLENV:SSH_AGENT_PID:SSH_AUTH_SOCK;
  ```
  * Open a window terminal and start Intellij from wsl (e.g ```wsl bash -lic "path-to-intellij"``` )
  * Open Intellij settings and set the ```Git``` executable to be the one installed in Windows Subsystem for linux
  * Run `rad auth` in your project directory


### Contribute changes (Creating a patch)

* Create a branch and commit your changes
* Open the Radicle Tool Window appearing on the left, or click <kbd>View</kbd> > <kbd>Tool Windows</kbd> > <kbd>Radicle</kbd>
* In the `Patches` tab, click on `+` on the top right of the Radicle Tool Window.
* Fill in your patch's title, description and labels
* Click on `Create Patch`.

Your new patch is now created and you can view it in the `Patches` tab!

### Accepting Contributed Changes (Merging a patch)

* Open a Radicle-initialised repo in your Jetbrains IDE.
* Use `Git -> Fetch` (or explicitly `Git -> Radicle -> Sync`) to fetch changes from the seed node. 
* Open the Radicle Tool Window appearing on the left, or click <kbd>View</kbd> > <kbd>Tool Windows</kbd> > <kbd>Radicle</kbd>
* In the `Patches` tab, you can view all available patches and filter them by State / Project / Author / Label.
* Select one of the `Open` patches, by double-clicking on it.
* Now you can view the patch information, including Patch ID, Author, Title, Description, as well as the patch's Timeline in the editor. You can also `Checkout` the patch and create a local git branch from it.
* While reviewing the patch changes, you can click on the `+` sign appearing in the gutter in the editor screen to add review comments.
* Once you feel the patch is ready to be merged, you can click on the `Merge` button from the patch info tab. This will merge the patch to your repository's default branch and push the changes to the radicle remote (rad).

The patch is now merged!
