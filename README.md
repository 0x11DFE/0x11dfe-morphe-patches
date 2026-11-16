# Waypoint Morphe Patches

Morphe patch source for **Waypoint** (`com.greycastel.waypoint`) - a React Native compass
app whose "Waypoint Pro" unlock (products `waypoint_lifetime` / `waypoint_monthly` /
`waypoint_yearly`, entitlement id `Waypoint Pro`) is enforced client-side through
RevenueCat (react-native-purchases 9.15.0) and protected by Google Play PairIP.

Supported version: **1.10.1** (versionCode 83).

## Patches

### Unlock Waypoint Pro
Two complementary edits:

1. **`RNPurchasesModule.getCustomerInfo(Promise)` is rewritten to resolve immediately**
   with a fabricated CustomerInfo map containing an active, lifetime `Waypoint Pro`
   entitlement. This is the primary unlock: the JS SDK calls it at startup, so the app
   sees Pro even when api.revenuecat.com is unreachable (DNS blockers, MITM, offline).
   Note the replacement body uses v0-v5, so the method register count is raised to 8.
2. **`EntitlementInfosMapperKt.map()`** is rewritten to return the same injected
   entitlement map. This covers the `Purchases-CustomerInfoUpdated` event path for
   installs where RevenueCat is reachable.

Why both: an earlier mapper-only variant did not unlock anything on devices where the RC
backend request fails (no CustomerInfo is ever delivered, so the mapper never runs).

### Disable PairIP license check
Empties `com.pairip.licensecheck.LicenseClient.checkLicense(Context)` (tries the
`licensecheck3` package variant as a fallback). Without this, PairIP kills a re-signed
APK. No `assets/pairipcore` VM layer exists in this app.

### Skip onboarding paywall
Patches `assets/index.android.bundle` (Hermes bytecode): the `/(onboarding)/paywall` route
component runs its env setup (router, `completeOnboarding` store action), then - instead of
rendering the PaywallSheet - invokes the app's own free-choice handler and returns null.
Effect: tutorial finishes straight into the app, `hasCompletedOnboarding` is genuinely
persisted by the app (no redirect loop), and the screen never appears. The patch matches a
24-byte window unique to this build and fails loudly if the bundle changed. Note: upgrade
upsell buttons elsewhere also just return to the app, consistent with an unlocked build.

## Update policy (v1.2.0+)

Patches are NOT version-pinned: they are offered for any APK and fail loudly when a
target is missing. After a Waypoint update:
- PairIP and RevenueCat patches usually keep working unchanged.
- For the bundle patch run `scripts/derive-paywall-pattern.py` on the new
  `assets/index.android.bundle` (needs `pip install hermes-dec`) and paste the printed
  `pattern` into `SkipOnboardingPaywallPatch.kt`.
To restore strict version gating, re-add `compatibleWith(COMPATIBILITY_WAYPOINT)`.

## Known limits (server-side, not unlockable locally)

- The MapLibre maps token worker (`waypoint-maps-token.pyerschris.workers.dev`) rejects
  requests without a real Pro entitlement ("Maps token denied: no active Pro entitlement").
- Cloud backup/sync and Rally live location depend on Waypoint's backend and may still
  require a real Pro account.

## Build

Requires JDK 17, Android SDK, and a GitHub token with `read:packages` access to
`MorpheApp/registry` (Morphe's patcher Gradle plugin is distributed via GitHub Packages).

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export GITHUB_ACTOR=<github user>
export GITHUB_TOKEN=<token with read:packages>
./gradlew :patches:buildAndroid          # -> patches/build/libs/patches-<version>.mpp
./gradlew :patches:generatePatchesList   # -> patches-list.json
```

## Install notes

- Morphe re-signs the APK with its own key: uninstall the Play-installed original first
  (back up app data - pins, settings, trails).
- Verified on-device against 1.10.1 by applying the identical three smali edits with
  MT Manager: pairip kill + bridge promise resolve + mapper injection; Pro unlocked with
  RevenueCat unreachable (`CertPathValidatorException` on every /v1/subscribers call).

## Version bumping

1. Open the new APK and confirm these still exist:
   - `Lcom/revenuecat/purchases/react/RNPurchasesModule;->getCustomerInfo(Lcom/facebook/react/bridge/Promise;)V`
   - `Lcom/revenuecat/purchases/hybridcommon/mappers/EntitlementInfosMapperKt;->map(Lcom/revenuecat/purchases/EntitlementInfos;)Ljava/util/Map;`
   - `Lcom/pairip/licensecheck/LicenseClient;->checkLicense(Landroid/content/Context;)V`
2. Check `RNPurchasesModule.PLUGIN_VERSION` (react-native-purchases). The mapper shape is
   stable across SDK 8.x/9.x releases; `getCustomerInfo` is a stable bridge method.
3. Add the new version string to `COMPATIBILITY_WAYPOINT` targets and rebuild.

## Publishing this repo as a Morphe patch source

Morphe Manager does not auto-discover repos: users add a source URL by hand. To ship
updates, CI builds the bundle and attaches it to a GitHub release.

1. Push this repo to GitHub (main branch).
2. No secrets required: the included workflow authenticates to the Morphe Gradle
   plugin registry (`maven.pkg.github.com/MorpheApp/registry`) with the default
   Actions `GITHUB_TOKEN`.
3. Tag a release: `git tag v1.0.0 && git push --tags`. The workflow builds
   `patches/build/libs/patches-<version>.mpp` plus `patches-list.json` and attaches
   them to the release.
4. In Morphe Manager, users add `https://github.com/<you>/<repo>` as a patch source.

To be listed in an existing community source instead, the two patch files, the
`COMPATIBILITY_WAYPOINT` constant, and `docs/waypoint/README.md` are drop-in for any
repo using the same Morphe template (e.g. rabilrbl/fluffy-patches) - open a PR there.
