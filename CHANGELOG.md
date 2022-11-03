<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# radicle-jetbrains-plugin Changelog

## Unreleased

## 0.2.1 - 2022-11-03

### Changed
- Release workflow improvements by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/119
- Show unsupported version warning if the rad cli version is not 0.6.1 by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/120

**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.2.0...v0.2.1

## 0.2.0 - 2022-11-02

### Added
- It's now possible to publish Git repos to Radicle, directly from your IDE !! https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/56 and https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/61
- You can now view/manage Radicle Identities in the plugin settings and choose your active Identity - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/43
- Chances are you will have some frequently-used seed nodes... You can now add these in the plugin settings - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/42
- * Browse through projects and clone them, from the new “Radicle” section in the existing Git clone dialog - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/45, https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/48 and https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/49
    * Copy the `rad://` URL from the Radicle Web Client and paste it in the IDE - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/60

### Changed
- Bump remoteRobotVersion from 0.11.15 to 0.11.16 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/73
- Bump org.jetbrains.intellij from 1.8.0 to 1.8.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/77
- Bump org.jetbrains.intellij from 1.8.1 to 1.9.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/79
- Bump junit-jupiter-api from 5.9.0 to 5.9.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/82
- Bump org.jetbrains.kotlin.jvm from 1.7.10 to 1.7.20 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/88
- Bump junit-jupiter-engine from 5.9.0 to 5.9.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/83
- Bump junit-vintage-engine from 5.9.0 to 5.9.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/80
- Bump junit-platform-launcher from 1.9.0 to 1.9.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/81
- Bump FedericoCarboni/setup-ffmpeg from 1 to 2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/100
- Bump JetBrains/qodana-action from 2022.2.1 to 2022.2.3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/110
- Bump mockito-core from 4.8.0 to 4.8.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/113
- Bump org.jetbrains.changelog from 1.3.1 to 2.0.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/112

## 0.1.4-alpha
- Add radicle icons in the navigation bar
- Check if the project is rad initialized before show the dialog
- Update windows instructions
- Update IDE version used in tests to 2022.2

## 0.1.3-alpha
- Show a notification every time the user open a new project, to remind him to configure the plugin path
- Fix a bug in settings
- Update icons

## 0.1.2-alpha
- Change java version from 17 to 11 and change platform version from 2022.2 to 2020.3

## 0.1.1-alpha

### Added
- Added Radicle menu and toolbar buttons by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/13
- Added Radicle section in settings by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/19
- Includes first non-UI Tests by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/25
- Includes first UI Tests by @gsaslis in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/18
- improve plugin description by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/31
- Adds usage instructions by @gsaslis in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/34
- Fix Draft Release GH Action by @gsaslis in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/35
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

### New Contributors
- @gsaslis made their first contribution in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/1
- @JChrist made their first contribution in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/12
- @Stelios123 made their first contribution in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/19

**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/commits/v0.1.0-alpha
