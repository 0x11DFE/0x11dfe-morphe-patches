# Scene (com.omarea.vtools) - Unlock N1

## What the unlock replicates

The known "Scene N1" mod for `N1 2026.10 Alpha13` edits exactly three methods in
`classes.dex` (verified by dex-diffing the mod against the original; no methods
added or removed):

| Purpose | 2026.10 Alpha13 | 2026.11 PreRelease 1 |
|---|---|---|
| License state parser | `La/c3;->b(Ljava/lang/String;)Lcom/omarea/model/ActivatedStateModel;` | same name, body identical apart from helper renames |
| License type from activation response | `Lcom/omarea/model/ActivationCodeResponse;->getType()Ljava/lang/String;` | unchanged |
| MD5 of APK signing cert (server-side activation binding) | `La/at1;->C(Landroid/content/Context;Ljava/lang/String;)Ljava/lang/String;` | `La/rv1;->B(...)` (class renamed) |

Obfuscated-helper renames between the two builds: `oi1 -> bl1`, `sj1 -> em1`,
`at1 -> rv1`, `at -> tt`.

## How it works

- `c3.b(input)` normally parses the stored license state string
  (`success@<expiry-millis>`, `expired`, `empty`, `invalid`, `not-you`) and builds
  an `ActivatedStateModel` (`activated`, `permanent`, `type`, `typeName`, `text`).
  The first three instructions load that state (`this.b.toString()`); the patch
  replaces them with `const-string v0, "success@35343792000000"` so every caller
  gets `activated = true` with expiry 3089-12-31. `c3.c()` (the v2 flow reading
  `activate_v2_type` from prefs) delegates to `b()`, so it is covered too.
- `ActivationCodeResponse.getType()` is forced to `"perpetual"` so any activation
  attempt stores the perpetual license type.
- The signature-MD5 getter is forced to `"34ea0e93b18c7df94decd663857bb3bf"`
  (MD5 of the original signing certificate). omarea binds activation codes to
  this hash, so a re-signed APK (Morphe/MT) must keep reporting the original.

## Updating to a new Scene build

1. Check the three targets above still exist (names may shift again - locate
   `c3` via its `"success@"` string, the MD5 helper via `"MD5"` + `getPackageInfo`).
2. Verify `c3.b` still starts with the three-instruction state load.
3. Add the new `versionName` to `COMPATIBILITY_SCENE` in `shared/Constants.kt`.
