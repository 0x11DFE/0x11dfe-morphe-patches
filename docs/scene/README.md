# Scene (com.omarea.vtools) - Unlock N1

## What the unlock replicates

The known "Scene N1" mod for `N1 2026.10 Alpha13` edits exactly three methods in
`classes.dex` (verified by dex-diffing the mod against the original; no methods
added or removed):

| Purpose | 2026.10 Alpha13 | 2026.11 PreRelease 1 | 2026.11 PreRelease 2 |
|---|---|---|---|
| License state parser | `La/c3;->b(Ljava/lang/String;)Lcom/omarea/model/ActivatedStateModel;` | same | same |
| License type from activation response | `Lcom/omarea/model/ActivationCodeResponse;->getType()Ljava/lang/String;` | unchanged | unchanged |
| MD5 of APK signing cert (server-side activation binding) | `La/at1;->C(...)Ljava/lang/String;` | `La/rv1;->B(...)` | `La/mw1;->B(...)` |

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
- The `expiry-date` patch option (yyyy-MM-dd, default 3089-12-31) is converted to
  epoch milliseconds at patch time and baked into that constant.
- `ActivationCodeResponse.getType()` is forced to `"perpetual"` so any activation
  attempt stores the perpetual license type.
- The signature-MD5 getter is forced to `"34ea0e93b18c7df94decd663857bb3bf"`
  (MD5 of the original signing certificate). omarea binds activation codes to
  this hash, so a re-signed APK (Morphe/MT) must keep reporting the original.

## Updating to a new Scene build

1. Check the three targets above still exist (names may shift again - locate
   `c3` via its `"success@"` string, the MD5 helper via `"MD5"` + `getPackageInfo`).
2. Verify `c3.b` still starts with the three-instruction state load.
3. Add the new `versionName` to `COMPATIBILITY_SCENE` in `shared/Constants.kt`
   and the renamed MD5 helper (if any) to the candidate list in the patch.
