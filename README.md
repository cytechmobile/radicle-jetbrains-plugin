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

**Known issue**: Due to latest changes in private repos, we are aware of issues working with private repos in the plugin. Please reach out to us in [#integrations](https://radicle.zulipchat.com/#narrow/stream/380896-integrations) for assistance, if you are facing this issue.

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
* A Radicle **identity** (create with `rad auth`. Check [here](https://docs.radicle.xyz/guides/user#come-into-being-from-the-elliptic-aether) for examples.)
* 2 running background processes: 
  * `radicle-node`: In order to sync your changes with the network. Please check [here](https://docs.radicle.xyz/guides/user#operate-nodes-smoothly) for instructions on operating your radicle node.
  * `radicle-httpd`: In order to browse project issues and patches and also be able to make changes to them, such as write comments, leave in-line code reviews, merge patches, etc.  
    * _IMPORTANT_: Please note that we are currently in the process of removing this dependency and moving everything to just the `rad` CLI, as part of our [v0.11 release](https://github.com/cytechmobile/radicle-jetbrains-plugin/milestone/16). In the meantime, this plugin won't work with the latest official version of `radicle-httpd`. You can use a forked version we have released [here](https://minio.radicle.gr/browser/radicle-releases/cmFkaWNsZS1odHRwLXNlcnZlci8=). Once you download the corresponding binary for your platform, you can run it with, e.g.: `radicle-httpd --listen 0.0.0.0:8080`. 

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
