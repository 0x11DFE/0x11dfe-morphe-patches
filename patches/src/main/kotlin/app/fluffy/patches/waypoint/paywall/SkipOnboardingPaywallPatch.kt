package app.fluffy.patches.waypoint.paywall

import app.fluffy.patches.shared.Constants.COMPATIBILITY_WAYPOINT
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import java.security.MessageDigest

@Suppress("unused")
val skipOnboardingPaywallPatch = rawResourcePatch(
    name = "Skip onboarding paywall",
    description = "The paywall screen shown after the tutorial immediately invokes the app's own free-choice handler (completeOnboarding + navigate home) and renders nothing. Also makes the in-app upgrade upsell buttons return to the app, which is consistent with an already-unlocked build.",
) {
    compatibleWith(COMPATIBILITY_WAYPOINT)

    execute {
        val bundle = get("assets/index.android.bundle")
            ?: throw PatchException("assets/index.android.bundle not found in APK")
        val bytes = bundle.readBytes()

        // Hermes bytecode window inside PaywallScreen (route component of /(onboarding)/paywall):
        // CreateClosure of the completeOnboarding selector, useAppStore(selector) call, the
        // StoreToEnvironment that binds the action, then the start of the PaywallSheet render path.
        val pattern = byteArrayOf(
            0x64, 0x00, 0x01, 0x43, 0x4d, 0x53, 0x00, 0x02, 0x03, 0x00,
            0x2a, 0x01, 0x01, 0x00, 0x2e, 0x00, 0x04, 0x00, 0x37, 0x00, 0x00, 0x04,
            0xd8.toByte(), 0xaa.toByte(),
        )
        // Replaces the last 10 bytes of the window: LoadConstUndefined r3; Call1 r0,r7,r3
        // (invoke handleDone); LoadConstNull r0; Ret r0.
        val replacement = byteArrayOf(
            0x76, 0x03, 0x51, 0x00, 0x07, 0x03, 0x77, 0x00, 0x5c, 0x00,
        )

        var index = -1
        var occurrences = 0
        var i = indexOf(bytes, pattern)
        while (i >= 0) {
            index = i
            occurrences++
            i = indexOf(bytes, pattern, i + 1)
        }
        if (occurrences != 1) {
            throw PatchException(
                "Expected exactly one PaywallScreen window in index.android.bundle, found $occurrences",
            )
        }

        val patched = bytes.copyOf()
        System.arraycopy(
            replacement, 0,
            patched, index + pattern.size - replacement.size,
            replacement.size,
        )

        // The bundle carries a SHA-1 trailer over everything before it; keep it consistent.
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(patched.copyOfRange(0, patched.size - 20))
        System.arraycopy(digest, 0, patched, patched.size - 20, 20)

        bundle.writeBytes(patched)
        println("Waypoint paywall skip: patched PaywallScreen at offset ${"0x%x".format(index)}")
    }
}

private fun indexOf(haystack: ByteArray, needle: ByteArray, fromIndex: Int = 0): Int {
    if (needle.isEmpty() || needle.size > haystack.size) return -1
    outer@ for (i in fromIndex..haystack.size - needle.size) {
        for (j in needle.indices) {
            if (haystack[i + j] != needle[j]) continue@outer
        }
        return i
    }
    return -1
}
