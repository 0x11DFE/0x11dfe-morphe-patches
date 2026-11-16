# Scene (com.omarea.vtools) - Unlock N1

## What the unlock replicates

The known "Scene N1" mod for `N1 2026.10 Alpha13` edits exactly three methods in
`classes.dex` (verified by dex-diffing the mod against the original; no methods
added or removed):

| Purpose | How it is located | Name history (2026.10 -> 2026.11 PR1 -> PR2) |
|---|---|---|
| License state parser | fingerprint: `(String)ActivatedStateModel` + strings `success@`, `yyyy-MM-dd` | `c3.b` in all builds |
| License type from activation response | direct target (not obfuscated) | `ActivationCodeResponse.getType()` in all builds |
| MD5 of APK signing cert (server-side activation binding) | fingerprint: `(Context, String)String` + string `MD5` | `at1.C` -> `rv1.B` -> `mw1.B` |
| Device ID getter | fingerprint: `()String` + string `random_id2` + declaring class has a static `LinkedHashMap` field | `at.a` -> `tt.a` -> `nu.a` |

Obfuscated classes are located by [fingerprints](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_2_1_fingerprinting.md)
(method signature + embedded strings + class shape), so R8 renames between builds do not
require patch changes - only the new `versionName` has to be pinned in `Constants.kt`.

Obfuscated-helper renames across builds:

- 2026.10 Alpha13: `oi1`, `sj1`, `at1`, `at`
- 2026.11 PreRelease 1: `bl1`, `em1`, `rv1`, `tt`
- 2026.11 PreRelease 2: `wl1`, `zm1`, `mw1`, `nu`

`La/c3;`, `ActivationCodeResponse` and `ActivatedStateModel` keep their names in
all three builds.

## How it works

- `c3.b(input)` normally parses the stored license state string
  (`success@<expiry-millis>`, `expired`, `empty`, `invalid`, `not-you`) and builds
  an `ActivatedStateModel` (`activated`, `permanent`, `type`, `typeName`, `text`).
  The first three instructions load that state (`this.b.toString()`); the patch
  replaces them with `const-string v0, "success@<millis>"` so every caller gets
  `activated = true`. `c3.c()` (the v2 flow reading `activate_v2_type` from prefs)
  delegates to `b()`, so it is covered too.
- The `expiry-date` patch option (yyyy-MM-dd, default 2077-01-01) is converted to
  epoch milliseconds at patch time and baked into that constant.
- The standalone "Random device ID" patch (off by default) rewrites the device-ID
  holder's `a()Ljava/lang/String;` (class renamed per build: `at` -> `tt` -> `nu`) to
  return `UUID.randomUUID().toString()`, so activation requests carry a fresh ID every
  time.
- `ActivationCodeResponse.getType()` is forced to `"perpetual"` so any activation
  attempt stores the perpetual license type.
- The signature-MD5 getter is forced to `"34ea0e93b18c7df94decd663857bb3bf"`
  (MD5 of the original signing certificate). omarea binds activation codes to
  this hash, so a re-signed APK (Morphe/MT) must keep reporting the original.

## Updating to a new Scene build

1. Add the new `versionName` to `COMPATIBILITY_SCENE` in `shared/Constants.kt`.
2. If the release fails to patch: verify the license state parser still starts
   with the three-instruction state load (`this.b.toString()`), and that the
   fingerprinted strings (`success@`, `yyyy-MM-dd`, `MD5`, `random_id2`) still
   appear in the expected methods.

## APK downloads

- Official: https://download.omarea.com/#/platform
- Mirror: https://omarea.lanzout.com/b0he4yszi
