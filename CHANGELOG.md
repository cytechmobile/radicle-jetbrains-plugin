<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# radicle-jetbrains-plugin Changelog

## [Unreleased]

## [0.11.0] - 2024-09-27

- Settings: Make the settings window responsive

- Status Bar: Show or hide the status bar based on whether the project is RAD initialized

- Radicle Clone Window: Remove the "Browse Projects" view

- Get issue list: Use cli in order to fetch issues instead of HTTPD

- Add / remove issue assignees: Use cli to perform these actions instead of HTTPD

- Add / remove issue labels: Use cli to perform these actions instead of HTTPD

- Add / remove patch labels: Use cli to perform these actions instead of HTTPD

- Change issue state: Use cli to perform this action instead of HTTPD

- Welcome screen clone: Add RAD Path and RAD Home text fields to the welcome screen so the user can clone a Radicle project
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.10.0...v0.11.0

## [0.10.0] - 2024-06-28

- StatusBar: Show node and httpd status
- settings: Check HTTPD API version and report incompatibilities
- patches & issues: Add copy to clipboard for patch/issue ID
- settings: Show warning for non-default RAD_HOME. On Linux/mac, users should make sure that the correct RAD_HOME path is picked up by the spawned git process. On Windows/WSL, chances are that non-default RAD_HOME won't be picked up by git.
- inline comments: Show  comments from previous revisions. If the line was changed between revisions, then also add an 'OUTDATED' label.
- Commit & files panel: Fetch commits prior to attempting to show diff/files changed. Resolves the issue that showed 0 file/diff changes on first load.
- patch tab: Fix patch description. In info (sidebar), truncate if too long (>100 with ellipsis). In timeline, use latest non-empty,
not only the one in the last revision.  Change "View Timeline" to "Open in Editor".
- patch: Fix patch merge button identifying delegates
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.9.2...v0.10.0

## [0.9.2] - 2024-05-13

- fix(api): Fix project delegates schema change. This makes the plugin compatible with rad version `1.0.0-rc.8`
- Specify BGT thread for rad track, avoiding generating error reports.
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.9.1...v0.9.2

## [0.9.1] - 2024-05-02

- feat: Support replies in inline comments by stelios. Now you can add a reply to an inline comment on a patch (i.e. a comment straight on the diff of your code).
- feat: Disable rad Buttons by stelios. Disable radicle buttons from Git menu in non-radicle-initialized repos. The radicle menu remains, in order to provide functionality for `rad-init`ializing a repo.
- fix(patch): fix repo/branch selector size by jchrist. When creating a patch and you opened the repo/branch selector dialog, it was a bit cut-off.
- fix: flaky tests by stelios. Stop failing randomly! (neither fail consistently, that is...)
- docs: fix reported minimum supported CLI version by jchrist. We're expecting v1 now! 0.8.0 is history!
- fix(patch): Fix discovery logic for rad remote when creating patch by jchrist. When attempting to find the radicle remote in the git config, the logic could sometimes match the wrong remote (e.g. a radicle namespaced remote).  
- fix(patches): search by patch id by jchrist. In the patch list view, you could filter patches by anything, except their ID. So, if you knew already exactly what patch you wanted to view, you had to keep scrolling to find it.
- fix(patches): disable merge for non-delegates by jchrist. Non-delegates were able to "merge" a patch, but there would hardly be a reason for them to do so. Disable the button, so that they know that this will not work as they might have imagined. Of course, a non-delegate can always perform this action on their own if they really wish to (i.e. merge the patch branch to their own default branch).
- fix(timeline): Use alias for self by jchrist. The comment text field used the did of the currently selected identity, instead of the alias.
- fix(patches): Fix diff for unknown commit by jchrist. When calculating commits for a patch, a (superseded) revision might contain commits that haven't been fetched. Ignore this error, as we're mostly interested for the latest revision.
- fix(timeline): Fix patch/issue web links by jchrist. Fix the links in the timeline to a patch/issue to point straight to the preferred seed node in the default browser.
- fix(issues): Fix assignees selection & filtering by stelios.
- fix(issues): Fix issue assignees by jchrist. 
- fix: Notification & Refresh tab stelios
- build: update deps and bump version jchrist
- build(deps): bump JetBrains/qodana-action from 2023.3.1 to 2024.1.2
- build(deps): bump org.jetbrains.intellij from 1.17.2 to 1.17.3
- chore: add missing changelog for 0.9.0 jchrist
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.9.0...v0.9.1

## [0.9.0] - 2024-03-28

- feat(patch): Add a review along with comments. Also show the reviews in the timeline.
- feat: Refresh only emoji panel when adding/removing reactions
- docs: Improve docs in README.md and fix references to radicle guide and repos.
- fix: Correctly match review comment based on *relative* path to repo root
- fix(settings): Auto-detect RAD_HOME and RAD_PATH and store only if the existing settings were empty
- fix: Allow deleting on patch review comment
- ci: Fix jobs and changelog generation
- fix: Make the plugin compatible with 232, current latest release for Android Studio

## [0.8.4] - 2024-03-13

- feat: Open file in browser by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/490
- fix patches info and timeline by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/495
- fix: Emojis Deserialization by @Stelios123 in 4f9fae6d72990ea744515525fa1dcd4423127c86
- release: bump version to 0.8.4 by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/498
- Changelog update - `v0.8.3` by @github-actions in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/488
- build(deps): update dependencies by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/494
- ci: Change jobs trigger to push by @JChrist 
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.8.3...v0.8.4

## [0.8.3] - 2024-02-21

- fix(Filters Compatibility): Patch / Issues filtering by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/479
- fix(settings): Fix label widths by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/482
- fix(comments): Remove deprecated by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/483
- fix(reactions): Resolve updated httpd reaction json model by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/478
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.8.2...v0.8.3

## [0.8.2] - 2024-02-06

- Changelog update - `v0.8.1` by @github-actions in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/474
- chore(version): bump to next release by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/475
- Limit intellij platform until build 2023.3.3
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.8.1...v0.8.2

## [0.8.1] - 2024-02-05

- Changelog update - `v0.8.0` by @github-actions in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/453
- Our first end-to-end tests by @gsaslis in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/423
- fix(RadicleToolWindow): Multiple radicle toolwindows by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/456
- fix(api): fix node id deserialization by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/467
- chore(version): bump version for next release by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/469
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.8.0...v0.8.1

## [0.8.0] - 2024-01-12

- Edit patch comments by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/388
- Create patch proposal by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/397
- Support attachments in issues / patches by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/415
- Edit issue comments by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/420
- merge patch by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/418
- inline comments by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/433
- Show a notification error if the httpd server is down by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/403
- Show the diff from the latest revision only by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/409
- Preselected Filters by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/417
- Fetch all issues / patches by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/383
- Add embeds parameter to create issue api by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/386
- Run `rad sync` synchronously by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/389
- Fix radicle tool window disappearing by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/402
- Patch Checkout by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/411
- Patch overview panel by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/435
- unencrypted identities by @gsaslis in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/437
- Review Comments & Session: by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/441
- Publish to radicle by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/444
- Publish Dialog by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/449
- Authenticate User by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/448
- Fix settings dialog in Rust Rover & GoLand Ides by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/452
- Changelog update - `v0.7.3` by @github-actions in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/382
- Bump org.jetbrains.intellij from 1.15.0 to 1.16.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/390
- Build/updates by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/428
- Update remote robot by @gsaslis in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/436
- Bump org.jetbrains:annotations from 24.0.1 to 24.1.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/421
- Bump org.jetbrains.intellij from 1.16.0 to 1.16.1 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/426
- Bump JetBrains/qodana-action from 2023.2.9 to 2023.3.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/430
- Bump github/codeql-action from 2 to 3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/431
- Bump actions/upload-artifact from 3 to 4 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/432
- Bump org.jetbrains.kotlin.jvm from 1.9.20 to 1.9.22 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/439
- bump FedericoCarboni/setup-ffmpeg from 2 to 3 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/445
- Patches/304a5512dae85c38dac47b600496302ccdcabad4 by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/451
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.7.3...v0.8.0

## [0.7.3] - 2023-09-28

- Synchronizing your Radicle project with the network is something that the `radicle-node` automagically takes care of you behind the scenes. But not always (as the node is sometimes not running, or offline, etc.) That's why it's important to allow the user to manually `rad sync` their project! 🔛  by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/376
- Let's be honest, markdown is much prettier when rendered! We weren't rendering, so this PR adds markdown support in Radicle Issue and Patch description and comments! 🤩  ( by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/375
- Small fix when we didn't show the radicle tool window after the user first published a project.  by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/374
- Few (successful) projects have less than 10 issues or patches. Our test projects did though(!!), so we were previously only loading the latest 10... That's now fixed, so the plugin is ready for some real-world projects! 1️⃣ 0️⃣  by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/378
- It's now possible to add arbitrary Decentralized Identifiers (DIDs) as assignees to an issue (and not just assign an issue to delegates) ✅  by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/379
- Improved the emoji reaction button look and feel, so it should hopefully be easier to add reactions now! 👌 by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/381
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.7.2...v0.7.3

## [0.7.2] - 2023-09-19

- Check out patches directly from the IDE! A new check out button, allows you to check out the patch branch, so you can work directly with the code in your working copy. by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/360
- You can now not only add reactions to patches / issues, but you can also - wait for it - **remove** them. No more stress when you accidentally misclick the embarassing wrong icon.  by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/361
- The issue descriptions and comments were all having a bit of a bad hair day, so it was time to help them out a little.  We also fixed the  order of the fields inside publish dialog to match the CLI order! by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/359

### Dependabot (Security) Updates

- Bump actions/checkout from 3 to 4 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/348
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.7.1...v0.7.2

## [0.7.1] - 2023-09-13

- You can now create new Radicle Issues from within your IDE ! 🎉  by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/350
- You can also add reactions 👍🏼 🎉 🚀  to comments (for both Patches and Issues !) ;)  by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/341
- The "Refresh" button would reset filters in the Issue listing. @JChrist (politely) explained that it shouldn't, in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/349
- The Radicle Tool Window Icon size was kind of... large. A short diet later, it's now back to normal! by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/351

### Dependabot (Security) Updates

- Bump org.jetbrains.kotlin.jvm from 1.9.0 to 1.9.10 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/334
- Bump org.jetbrains.changelog from 2.1.2 to 2.2.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/336
- Bump org.jetbrains.dokka from 1.8.20 to 1.9.0 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/342
- Bump JetBrains/qodana-action from 2023.2.1 to 2023.2.6 by @dependabot in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/347
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.7.0...v0.7.1

## [0.7.0] - 2023-08-31

- Change the state / label of a patch proposal by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/325
- Support IDEA 2023.2 by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/329
**Full Changelog**: https://github.com/cytechmobile/radicle-jetbrains-plugin/compare/v0.6.2...v0.7.0

## [0.6.2] - 2023-08-23

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

## [0.6.1] - 2023-07-13

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

## [0.6.0] - 2023-06-27

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

## [0.5.0] - 2023-06-06

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

## [0.4.0] - 2023-04-05

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

## [0.3.0-alpha] - 2023-01-27

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

## [0.2.2] - 2022-11-04

- Fix changelog update during release by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/122

## [0.2.1] - 2022-11-03

### Changed

- Release workflow improvements by @JChrist in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/119
- Show unsupported version warning if the rad cli version is not 0.6.1 by @Stelios123 in https://github.com/cytechmobile/radicle-jetbrains-plugin/pull/120

## [0.2.0] - 2022-11-02

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

## [0.1.4-alpha]

- Add radicle icons in the navigation bar
- Check if the project is rad initialized before show the dialog
- Update windows instructions
- Update IDE version used in tests to 2022.2

## [0.1.3-alpha]

- Show a notification every time the user open a new project, to remind him to configure the plugin path
- Fix a bug in settings
- Update icons

## [0.1.2-alpha]

- Change java version from 17 to 11 and change platform version from 2022.2 to 2020.3

## [0.1.1-alpha]

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

[Unreleased]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.11.0...HEAD
[0.11.0]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.10.0...v0.11.0
[0.10.0]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.9.2...v0.10.0
[0.9.2]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.9.1...v0.9.2
[0.9.1]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.9.0...v0.9.1
[0.9.0]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.8.4...v0.9.0
[0.8.4]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.8.3...v0.8.4
[0.8.3]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.8.2...v0.8.3
[0.8.2]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.8.1...v0.8.2
[0.8.1]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.8.0...v0.8.1
[0.8.0]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.7.3...v0.8.0
[0.7.3]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.7.2...v0.7.3
[0.7.2]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.7.1...v0.7.2
[0.7.1]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.7.0...v0.7.1
[0.7.0]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.6.2...v0.7.0
[0.6.2]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.6.1...v0.6.2
[0.6.1]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.6.0...v0.6.1
[0.6.0]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.5.0...v0.6.0
[0.5.0]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.4.0...v0.5.0
[0.4.0]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.3.0-alpha...v0.4.0
[0.3.0-alpha]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.2.2...v0.3.0-alpha
[0.2.2]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.2.1...v0.2.2
[0.2.1]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.2.0...v0.2.1
[0.2.0]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.1.4-alpha...v0.2.0
[0.1.4-alpha]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.1.3-alpha...v0.1.4-alpha
[0.1.3-alpha]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.1.2-alpha...v0.1.3-alpha
[0.1.2-alpha]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/compare/v0.1.1-alpha...v0.1.2-alpha
[0.1.1-alpha]: https://app.radicle.xyz/nodes/seed.radicle.garden/rad:z3WHS4GSf8hChLjGYfPkJY7vCxsBK/commits/v0.1.1-alpha
