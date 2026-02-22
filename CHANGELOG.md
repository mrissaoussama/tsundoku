# Changelog

All notable changes to this project will be documented in this file.

The format is a modified version of [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
- `Added` - for new features.
- `Changed ` - for changes in existing functionality.
- `Improved` - for enhancement or optimization in existing functionality.
- `Removed` - for now removed features.
- `Fixed` - for any bug fixes.
- `Other` - for technical stuff.

## [Unreleased]
### Added
- Automatically remove downloads on Suwayomi after reading, configurable via extension settings ([@cpiber](https://github.com/cpiber)) ([#2673](https://github.com/mihonapp/mihon/pull/2673))
- Display author & artist name in MAL search results ([@MajorTanya](https://github.com/MajorTanya)) ([#2833](https://github.com/mihonapp/mihon/pull/2833))
- Add filter options to Updates tab ([@MajorTanya](https://github.com/MajorTanya)) ([#2851](https://github.com/mihonapp/mihon/pull/2851))
- Add bookmarked chapters to chapter download options ([@NarwhalHorns](https://github.com/NarwhalHorns)) ([#2891](https://github.com/mihonapp/mihon/pull/2891))
- Add `src:` prefix to search the library by source ID ([@MajorTanya](https://github.com/MajorTanya)) ([#2927](https://github.com/mihonapp/mihon/pull/2927))
  - Add `src:local` as a way to search for Local Source entries ([@MajorTanya](https://github.com/MajorTanya)) ([#2928](https://github.com/mihonapp/mihon/pull/2928))

### Improved
- Minimize memory usage by reducing in-memory cover cache size ([@Lolle2000la](https://github.com/Lolle2000la)) ([#2266](https://github.com/mihonapp/mihon/pull/2266))
- Optimize MAL search queries ([@MajorTanya](https://github.com/MajorTanya)) ([#2832](https://github.com/mihonapp/mihon/pull/2832))
- Reword download reindexing message to avoid confusion ([@MajorTanya](https://github.com/MajorTanya)) ([#2874](https://github.com/mihonapp/mihon/pull/2874))
- Rework internals for better performance ([@Lolle2000la](https://github.com/Lolle2000la)) ([#2955](https://github.com/mihonapp/mihon/pull/2955))
- Optimize tracked library filter ([@NarwhalHorns](https://github.com/NarwhalHorns)) ([#2977](https://github.com/mihonapp/mihon/pull/2977))
- Utilize tracker for library duplicate detection ([@NarwhalHorns](https://github.com/NarwhalHorns)) ([#2977](https://github.com/mihonapp/mihon/pull/2978))

### Changed
- Update tracker icons ([@AntsyLich](https://github.com/AntsyLich)) ([#2773](https://github.com/mihonapp/mihon/pull/2773))
- Add a small increment to chapter number before comparison to fix progress sync issues for Suwayomi ([@cpiber](https://github.com/cpiber)) ([#2657](https://github.com/mihonapp/mihon/pull/2675))

### Fixed
- Fix reader tap zones triggering after scrolling is stopped by tapping ([@NGB-Was-Taken](https://github.com/NGB-Was-Taken)) ([#2680](https://github.com/mihonapp/mihon/pull/2680))
- Fix shizuku installer not updating installed extensions ([@NGB-Was-Taken](https://github.com/NGB-Was-Taken)) ([#2697](https://github.com/mihonapp/mihon/pull/2697))
- Fix mass migration not using the same search queries as individual migration ([@AntsyLich](https://github.com/AntsyLich)) ([#2736](https://github.com/mihonapp/mihon/pull/2736))
- Fix reader not saving read duration when changing chapter ([@AntsyLich](https://github.com/AntsyLich), [@KotlinHero](https://github.com/KotlinHero)) ([#2784](https://github.com/mihonapp/mihon/pull/2784))
- Fix pre-1970 upload date display in chapter list ([@MajorTanya](https://github.com/MajorTanya)) ([#2779](https://github.com/mihonapp/mihon/pull/2779))
- Fix crash when trying to install/update extensions while shizuku is not running ([@NGB-Was-Taken](https://github.com/NGB-Was-Taken)) ([#2837](https://github.com/mihonapp/mihon/pull/2837))
- Fix Add Repo input not taking up the full dialog width ([@cuong-tran](https://github.com/cuong-tran)) ([#2816](https://github.com/mihonapp/mihon/pull/2816))

### Other
- Enable logcat logging on stable and debug builds without enabling verbose logging ([@NGB-Was-Taken](https://github.com/NGB-Was-Taken)) ([#2836](https://github.com/mihonapp/mihon/pull/2836))

## [v0.19.3] - 2025-11-07
### Improved
- Add lnreader-based grey color scheme [@Rojikku](https://github.com/Rojikku) [#2](https://github.com/tsundoku-otaku/tsundoku/pull/2)
- Rebrand from Mihon [@Rojikku](https://github.com/Rojikku) [#5](https://github.com/tsundoku-otaku/tsundoku/pull/5)

### Added
- Novel reading features [@mrissaoussamau](https://github.com/mrissaoussama)
- Support lnreader JS plugins [@mrissaoussamau](https://github.com/mrissaoussama)
- Infinite load reading [@mrissaoussamau](https://github.com/mrissaoussama)
- Import lnreader backups [@mrissaoussamau](https://github.com/mrissaoussama)
- Download rate limiting options [@mrissaoussamau](https://github.com/mrissaoussama)
- EPUB support [@mrissaoussamau](https://github.com/mrissaoussama)
- TTS [@mrissaoussamau](https://github.com/mrissaoussama)


## [Mihon]
This project was originally forked from Mihon, and, while we keep separate version numbers, we would like to note in our changelog when we merge fro upstream, and link to their changelog to try to give appropriate credit.
This project is greatly advantaged by building off all of their work, and their continued contributions!

Forked from Mihon v0.19.3 [7161bc2](https://github.com/mihonapp/mihon/commit/7161bc2e825bdfd66a1829f7dce42bd0570b1008)

[mihon]: https://github.com/mihonapp/mihon/blob/7161bc2e825bdfd66a1829f7dce42bd0570b1008/CHANGELOG.md