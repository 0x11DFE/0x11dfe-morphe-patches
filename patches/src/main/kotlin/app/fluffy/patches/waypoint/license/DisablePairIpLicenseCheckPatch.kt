package app.fluffy.patches.waypoint.license

import app.fluffy.patches.waypoint.clearBody
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disablePairIpLicenseCheckPatch = bytecodePatch(
    name = "Disable PairIP license check",
    default = false,
    description = "Empties com.pairip.licensecheck.LicenseClient.checkLicense so the re-signed patched APK is not killed by Google Play PairIP license enforcement. Generic: works on any PairIP-protected app with this class.",
) {
    execute {
        val licenseClient = sequenceOf(
            "Lcom/pairip/licensecheck/LicenseClient;",
            "Lcom/pairip/licensecheck3/LicenseClient;",
        ).mapNotNull { runCatching { mutableClassDefBy(it) }.getOrNull() }
            .firstOrNull()
            ?: throw IllegalStateException("PairIP LicenseClient class not found")

        licenseClient.methods
            .first { it.name == "checkLicense" && it.returnType == "V" }
            .apply {
                clearBody()
                addInstructions(0, "return-void")
            }
    }
}
