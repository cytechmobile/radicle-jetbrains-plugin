<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# radicle-jetbrains-plugin Changelog

## Unreleased

## 0.6.2 - 2023-08-23

### Fixes
- Fix broken api requests by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/324

- Fix `rad self` parsing by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/313

- Add alias field in the settings by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/315
Others: 

- Wait for data before calling the filter method on issues / patches by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/304

- Issue overview panel by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/303

- Change icons with new ones by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/316

- Remove classes from Utils and move to separate files by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/318

### Dependabot (Security) Updates
- Bump JetBrains/qodana-action from 2023.1.5 to 2023.2.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/305

- Bump org.junit.jupiter:junit-jupiter-engine from 5.9.3 to 5.10.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/307

- Bump org.junit.vintage:junit-vintage-engine from 5.9.3 to 5.10.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/309

- Bump org.junit.jupiter:junit-jupiter-api from 5.9.3 to 5.10.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/310

- Bump org.junit.platform:junit-platform-launcher from 1.9.3 to 1.10.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/308

- Bump org.jetbrains.kotlinx.kover from 0.7.2 to 0.7.3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/314
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.6.1...v0.6.2

## 0.6.1 - 2023-07-13
- Adds a refresh button for patches and issues by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/300
- Refactor issue & patch filters. by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/289

### Dependabot (Security) Updates
- Bump remoteRobotVersion from 0.11.18 to 0.11.19 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/292
- Bump org.jetbrains.kotlin.jvm from 1.8.22 to 1.9.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/291
- Bump org.jetbrains.kotlinx.kover from 0.7.1 to 0.7.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/288
- Bump org.jetbrains.changelog from 2.1.0 to 2.1.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/296
- Bump org.jetbrains.intellij from 1.14.2 to 1.15.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/295
- add support for aliases in radicle identities by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/293
- Bump org.jetbrains.changelog from 2.1.1 to 2.1.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/297
- bump version to 0.6.1 by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/301
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.6.0...v0.6.1

## 0.6.0 - 2023-06-27

### Features
- Radicle Issues [NEW]

  * Add list of Radicle Issues in Radicle Tool Window - #125
  * Add filters & search functionality in Issues panel - #124 
  * Show Issue overview and activity - #126 , #127 
- Radicle Patches [IMPROVEMENTS]

  * Add support for authenticating and using `radicle-httpd` API when _write_ access is required, initially for changing patch title by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/279
  * Add comment to Patch Proposal via HTTP API by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/283
  * Refactor patch proposals panel by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/267

### Dependabot (Security) Updates
- Bump org.jetbrains.changelog from 2.0.0 to 2.1.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/263
- Bump com.fasterxml.jackson.datatype:jackson-datatype-jsr310 from 2.15.1 to 2.15.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/261
- Bump org.mockito:mockito-core from 5.3.1 to 5.4.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/278
- Bump JetBrains/qodana-action from 2023.1.0 to 2023.1.4 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/277
- Bump JetBrains/qodana-action from 2023.1.4 to 2023.1.5 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/282
- Bump org.jetbrains.intellij from 1.13.3 to 1.14.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/275
- Bump org.jetbrains.kotlin.jvm from 1.8.10 to 1.8.22 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/274
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.5.0...v0.6.0

## 0.5.0 - 2023-06-06

### Features
- * with search / filtering - #130
- Radicle Patch Activity overview migrated to Heartwood - #134
- Radicle Patch Details migrated to Heartwood - #131
- Clickable commit hashes in patch proposal conversation view - #227
- New "Radicle -> Track" action: You can now track both Radicle projects and peers - #194
- Moved passphrase to `CredentialsStore`, provided by the Jetbrains IDE Plugin Software Development Kit (SDK) - #257 , #248 , #260

### Known Issues
- Handling special characters in patch proposal title and comments - #251

### Dependabot (Security) Updates
- Bump org.junit.platform:junit-platform-launcher from 1.9.2 to 1.9.3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/236
- Bump JetBrains/qodana-action from 2022.3.4 to 2023.1.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/232
- Bump com.squareup.okhttp3:logging-interceptor from 4.10.0 to 4.11.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/230
- Bump org.junit.jupiter:junit-jupiter-engine from 5.9.2 to 5.9.3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/239
- Bump org.mockito:mockito-core from 5.2.0 to 5.3.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/231
- Bump org.junit.jupiter:junit-jupiter-api from 5.9.2 to 5.9.3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/238
- Bump org.junit.vintage:junit-vintage-engine from 5.9.2 to 5.9.3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/235
- Bump com.fasterxml.jackson.datatype:jackson-datatype-jsr310 from 2.15.0 to 2.15.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/249
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.4.0...v0.5.0

## 0.4.0 - 2023-04-05

### Features
- * `rad clone` - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/189
  * `rad pull`(removed - happens with `git pull` now) - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/190
  * `rad sync` - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/193
  * `rad auth` (remove management of multiple identities) - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/192
  * `rad push` (removed - happens with `git push rad` now) - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/191
- Replace seed node management with configuration to seed node HTTP API URl - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/203
- Make `RAD_HOME` per-project-configurable instead of an application-wide setting - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/206
- Change `rad` CLI compatible version to 0.8.0+ by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/215
- Add package prefix to settings keys by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/218
- Hide Radicle Tool Window to avoid confusion while Patch Proposals are not migrated on Heartwood by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/217
- Find RAD_HOME without RAD_PATH being in the settings by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/220

### Bugfixes
- Unclear what module is being shared to Radicle - https://github.com/cytechmobile/radicle-jetbrains-plugin/issues/143

### Dependabot (Security) Updates
- Bump assertj-core from 3.23.1 to 3.24.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/159
- Bump mockito-core from 4.10.0 to 4.11.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/156
- Bump org.jetbrains.kotlin.jvm from 1.7.22 to 1.8.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/157
- Bump junit-vintage-engine from 5.9.1 to 5.9.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/161
- Bump junit-jupiter-api from 5.9.1 to 5.9.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/164
- Bump junit-platform-launcher from 1.9.1 to 1.9.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/162
- Bump junit-jupiter-engine from 5.9.1 to 5.9.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/163
- Bump JetBrains/qodana-action from 2022.3.0 to 2022.3.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/180
- Bump org.jetbrains.intellij from 1.12.0 to 1.13.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/199
- Bump org.jetbrains.kotlin.jvm from 1.8.0 to 1.8.10 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/183
- Bump JetBrains/qodana-action from 2022.2.4 to 2022.3.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/146
- Bump org.jetbrains.intellij from 1.10.1 to 1.11.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/154
- Bump JetBrains/qodana-action from 2022.2.3 to 2022.2.4 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/139
- Bump jtalk/url-health-check-action from 2 to 3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/135
- Bump org.jetbrains.intellij from 1.9.0 to 1.10.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/137
- Bump mockito-core from 4.8.1 to 4.9.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/136
- Bump mockito-core from 4.9.0 to 4.10.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/151
- Bump org.jetbrains.kotlin.jvm from 1.7.20 to 1.7.22 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-
- Bump org.jetbrains.intellij from 1.11.0 to 1.12.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/168
- Bump assertj-core from 3.24.1 to 3.24.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/171
- Bump org.jetbrains.intellij from 1.13.2 to 1.13.3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/208
- Bump mockito-core from 4.11.0 to 5.0.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/167
- Bump JetBrains/qodana-action from 2022.3.2 to 2022.3.4 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/186
- Bump remoteRobotVersion from 0.11.16 to 0.11.18 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/198
- Bump org.mockito:mockito-core from 5.0.0 to 5.2.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/200
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.3.0...v0.4.0

## 0.3.0-alpha - 2023-01-27
- New Radicle Tool Window added to IDE by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/153, https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/172
- Add Patch Proposals to Radicle Tool Window by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/160
- Improve visual styling and change rad icons in the new UI (Light / Dark theme) by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/166
- Bump java version from 11 to 17 when preparing plugin for release by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/173
- Compatibility improvements for latest IntelliJ versions by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/177
- Move 0.3.0 release to alpha release stream by @gsaslis in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/174
- Checkstyles by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/165
- Hide rad icons from the toolbar if the project is not git initialised by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/179

### Bumps
- Bump JetBrains/qodana-action from 2022.2.3 to 2022.2.4 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/139
- Bump jtalk/url-health-check-action from 2 to 3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/135
- Bump org.jetbrains.intellij from 1.9.0 to 1.10.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/137
- Bump mockito-core from 4.8.1 to 4.9.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/136
- Bump mockito-core from 4.9.0 to 4.10.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/151
- Bump org.jetbrains.kotlin.jvm from 1.7.20 to 1.7.22 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/138
- Bump JetBrains/qodana-action from 2022.2.4 to 2022.3.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/146
- Bump org.jetbrains.intellij from 1.10.1 to 1.11.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/154
- Bump assertj-core from 3.23.1 to 3.24.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/159
- Bump mockito-core from 4.10.0 to 4.11.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/156
- Bump org.jetbrains.kotlin.jvm from 1.7.22 to 1.8.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/157
- Bump junit-vintage-engine from 5.9.1 to 5.9.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/161
- Bump junit-jupiter-api from 5.9.1 to 5.9.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/164
- Bump junit-platform-launcher from 1.9.1 to 1.9.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/162
- Bump junit-jupiter-engine from 5.9.1 to 5.9.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/163
- Bump org.jetbrains.intellij from 1.11.0 to 1.12.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/168
- Bump assertj-core from 3.24.1 to 3.24.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/171
- Bump JetBrains/qodana-action from 2022.3.0 to 2022.3.2 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/180
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.2.2...v0.3.0-alpha

## 0.2.2 - 2022-11-04
- Fix changelog update during release by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/122

## 0.2.1 - 2022-11-03

### Changed
- Release workflow improvements by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/119
- Show unsupported version warning if the rad cli version is not 0.6.1 by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/120

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
