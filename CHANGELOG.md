## [1.5.0](https://github.com/0x11DFE/morphe-patches/compare/v1.4.0...v1.5.0) (2026-11-16)

### New Features

* Scene random device ID option, default expiry 2077-01-01, APK download sources ([34372d2](https://github.com/0x11DFE/morphe-patches/commit/34372d2330c14cbb38ebbaa9b4ab36dd7db32c6e))

### Bug Fixes

* nullable boolean option check compiles ([fe7ec75](https://github.com/0x11DFE/morphe-patches/commit/fe7ec758c8f096b49af84cc59735b98fb1fef547))

## [1.4.0](https://github.com/0x11DFE/morphe-patches/compare/v1.3.0...v1.4.0) (2026-11-16)

### New Features

* Scene unlock expiry-date option and 2026.11 PreRelease 2 support ([bafbe78](https://github.com/0x11DFE/morphe-patches/commit/bafbe78743817987fae4e2a6e86fc6adb70732f5))

## [1.3.0](https://github.com/0x11DFE/0x11dfe-morphe-patches/compare/v1.2.4...v1.3.0) (2026-11-16)

### New Features

* add Scene N1 unlock (license state, type and signature spoof) ([a31b694](https://github.com/0x11DFE/0x11dfe-morphe-patches/commit/a31b69480df7f3c7f0b8709e0a4d3f076f27158c))

## [1.2.4](https://github.com/0x11DFE/waypoint-morphe-patches/compare/v1.2.3...v1.2.4) (2026-11-16)

### Bug Fixes

* package-scope the three patches to Waypoint 1.10.1 so it lists in the Morphe catalog ([ddc48fd](https://github.com/0x11DFE/waypoint-morphe-patches/commit/ddc48fd393f026f3eafbe1d152fd018fc8b60243))

## [1.2.3](https://github.com/0x11DFE/waypoint-morphe-patches/compare/v1.2.2...v1.2.3) (2026-11-16)

### Bug Fixes

* declare universal patches default false ([7948402](https://github.com/0x11DFE/waypoint-morphe-patches/commit/7948402d2da38d43acce3ada033371eae6c732c3))

## [1.2.2](https://github.com/0x11DFE/waypoint-morphe-patches/compare/v1.2.1...v1.2.2) (2026-11-16)

### Bug Fixes

* migrate to official Morphe semantic-release pipeline ([fc639fd](https://github.com/0x11DFE/waypoint-morphe-patches/commit/fc639fd84b0361f2ec6d68d467bf2fe61c43d6d6))

# Changelog

## 1.2.1

- fix: restore version (gradle.properties was empty), derive bundle URL from artifact

## 1.2.0

- v1.2.0: unpin versions, add paywall pattern derivation script

## 1.1.0

- feat: skip onboarding paywall (bundle byte patch)
- fix: use rawResourcePatch + get() API for bundle byte patch

## 1.0.0

- Waypoint 1.10.1: Unlock Waypoint Pro + Disable PairIP license check
