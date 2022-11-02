# Developer notes

## Changelog management

* Bump plugin version in gradle.properties and open PR with version bump
* After PR is merged, draft_release GH action runs
  * Generates draft release with auto-generated release notes (from PRs)
* Developer edits release notes in draft release (from GitHub Web UI)
* Developer publishes release
* Release GH action runs 
  * picks up changelog from GH release body
  * updates plugin.xml (:patchPluginXml gradle task - which runs as part of :publishPlugin gradle task)
  * updates changelog in CHANGELOG.md (:patchChangelog gradle task)  
  * publishes plugin to Jetbrains marketplace
  * creates PR with changelog entries
