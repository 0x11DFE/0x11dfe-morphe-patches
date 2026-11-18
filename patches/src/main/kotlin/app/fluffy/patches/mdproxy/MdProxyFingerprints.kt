package app.fluffy.patches.mdproxy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * TimeLeft getter (zq0.B on v1.0.8.8): the only parameterless static method
 * returning a long that reads the "TimeLeft" MMKV key (decodeLong with a
 * 1-hour default). Every other "TimeLeft" mention either encodes it back
 * (MMKV.h) or returns void. The helper class name is R8-random per build;
 * the fingerprint does not depend on it.
 */
object TimeLeftGetterFingerprint : Fingerprint(
    isStatic = true,
    returnType = "J",
    parameters = emptyList(),
    filters = listOf(
        string("TimeLeft", StringComparisonType.CONTAINS),
        methodCall(definingClass = "Lcom/tencent/mmkv/MMKV;", name = "c"),
    ),
)

/**
 * Session countdown formatter (HomeFragment.v0): formats the remaining
 * milliseconds as "%02d:%02d:%02d:%02d" (days:hours:minutes:seconds). The
 * only method with that format string and a long parameter.
 */
object CountdownFormatterFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Ljava/lang/String;",
    parameters = listOf("J"),
    filters = listOf(
        string("%02d:%02d:%02d:%02d", StringComparisonType.CONTAINS),
    ),
)

/**
 * Rewarded-ad loader on the home screen (HomeFragment.n0): parameterless,
 * loads ad unit .../7843511161 through the HomeFragment$loadRewardedAd$1
 * callback. The server-row click handler loads the same unit but through
 * ServerAdapter$loadRewardedAd$1, which keeps the two apart.
 */
object HomeRewardedAdLoaderFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        string("ca-app-pub-7120795588295806/7843511161", StringComparisonType.CONTAINS),
        methodCall(definingClass = "Lcom/v2ray/ang/ui/HomeFragment$loadRewardedAd$1;", name = "<init>"),
    ),
)

/**
 * Interstitial loader (HomeFragment.k0): the only method that references
 * interstitial unit .../3815235509.
 */
object InterstitialAdLoaderFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        string("ca-app-pub-7120795588295806/3815235509", StringComparisonType.CONTAINS),
    ),
)

/**
 * Banner loader launcher (HomeFragment.j0): carries no string constants;
 * identified by launching the loadBannerAds$1 coroutine. Only j0 constructs
 * that callback class.
 */
object BannerAdLoaderFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = "Lcom/v2ray/ang/ui/HomeFragment$loadBannerAds$1;", name = "<init>"),
    ),
)

/**
 * App-open ad loader (AngApplication$AppOpenAdManager.a): the only method
 * referencing app-open unit .../6054386034.
 */
object AppOpenAdLoaderFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroidx/fragment/app/FragmentActivity;"),
    filters = listOf(
        string("ca-app-pub-7120795588295806/6054386034", StringComparisonType.CONTAINS),
    ),
)

/**
 * Server-row boost click handler (R8-merged lambda, m61 on v1.0.8.8): the
 * packed-switch onClick that boosts a server row by loading rewarded unit
 * .../7843511161. "Ads are not available yet" is shared with the main
 * click handler, but only this method also carries the ad-unit constant.
 */
object ServerRowBoostClickFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        string("Ads are not available yet", StringComparisonType.CONTAINS),
        string("ca-app-pub-7120795588295806/7843511161", StringComparisonType.CONTAINS),
    ),
)

/**
 * Boost / periodic-interstitial gate (HomeFragment.C0): static, takes
 * (HomeFragment, FragmentActivity, boolean showBoost). Reads CurrentVoucher
 * (holders skip it), formats a date and reads the BoostedServer array for
 * the grant-on-interstitial-shown path. The parameter list alone is unique;
 * the two strings pin it.
 */
object BoostInterstitialFlowFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf(
        "Lcom/v2ray/ang/ui/HomeFragment;",
        "Landroidx/fragment/app/FragmentActivity;",
        "Z",
    ),
    filters = listOf(
        string("CurrentVoucher", StringComparisonType.CONTAINS),
        string("yyyy-MM-dd HH:mm:ss", StringComparisonType.CONTAINS),
    ),
)

/**
 * Selected-server getter (zq0.D on v1.0.8.8): the only parameterless static
 * String method reading the "USER_SELECTED_SERVER" MMKV key.
 */
object SelectedServerGetterFingerprint : Fingerprint(
    isStatic = true,
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        string("USER_SELECTED_SERVER", StringComparisonType.CONTAINS),
    ),
)
