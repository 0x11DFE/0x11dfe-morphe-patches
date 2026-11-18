# MD PROXY VPN (com.vpnproxymd.mdproxyvpn)

A v2rayNG fork bundled with an SSH/OpenVPN tunnel stack, AdMob and a
voucher/session-time monetization layer. Supported version: v1.0.8.8
(versionCode 29).

The app keeps its `com.v2ray.ang` class names, but the MMKV helper class and
the R8-merged click-handler lambdas are renamed on every build. All patch
targets are located by content fingerprints (ad-unit constants, MMKV keys,
format strings), never by obfuscated names, and the Speed Boost rewrite
resolves its helper calls from the app's own bytecode at patch time.

## Unlimited Time

Sessions are metered by an MMKV `TimeLeft` value (1 hour default, reset to 10
minutes at connect when it runs low, decremented by a countdown service that
stops the VPN when it reaches zero). The patch replaces the getter so it always
returns 100 years, which also skips the low-time reset at connect, and prepends
an infinity branch to the countdown formatter so the display shows `∞` instead
of a 6-digit day count. Voucher validity and the rewarded-ad time top-up flow
are left untouched and simply become unnecessary.

## Remove Ads

Empties the four app-side ad loaders (banner coroutine, interstitial,
home rewarded, app-open) and NOPs the `RewardedAd.load` call inside the
server-row boost button. No ad request leaves the device afterwards; the
interstitial show sites are null-guarded in the app, so nothing crashes. The
server-row handler also routes server selection, so only the ad load is
neutralized, not the whole click handler.

## Unlock Speed Boost

The per-server "Speed Boost" is granted when an interstitial ad actually
shows; the app also re-shows that interstitial every 3 minutes while
connected. The patch rewrites the gate method: a boost tap writes the selected
server into the `BoostedServer` MMKV list directly (same storage the ad flow
uses, so the rocket badge and per-row state behave identically) and shows the
`Server Boosted!!!` toast; non-boost callers of the gate return immediately,
which also silences the periodic interstitial. Works together with
"Remove Ads".
