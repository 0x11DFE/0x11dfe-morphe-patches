# Waypoint

Waypoint (com.greycastel.waypoint) is a React Native outdoor navigation / compass app.
"Waypoint Pro" is a one-time (and monthly/yearly) unlock enforced client-side via
RevenueCat (react-native-purchases 9.x); the app is additionally protected by Google
Play PairIP.

| Patch | Description |
|-------|-------------|
| Unlock Waypoint Pro | Resolves the RN bridge `getCustomerInfo` promise with a fabricated CustomerInfo carrying an active lifetime `Waypoint Pro` entitlement (product `waypoint_lifetime`), and injects the same entitlement in the hybridcommon mapper. Works even when api.revenuecat.com is unreachable. |
| Disable PairIP license check | Empties `com.pairip.licensecheck.LicenseClient.checkLicense` so re-signed builds are not killed. |
| Skip onboarding paywall | Patches the Hermes bundle so the post-tutorial paywall route instantly runs the app's free-choice handler (completeOnboarding + navigate home) and renders nothing. |

## Supported versions

| Version |
|---------|
| 1.10.1  |

## Notes

- Server-side features are not unlockable: the maps token worker rejects requests
  without a real entitlement, and cloud backup / sync / Rally depend on Waypoint's
  backend.
- Install requires uninstalling the Play original first (signature differs); back up
  pins and settings.
