package app.fluffy.patches.scene.privacy

import app.fluffy.patches.scene.DeviceIdGetterFingerprint
import app.fluffy.patches.shared.Constants.COMPATIBILITY_SCENE
import app.fluffy.patches.waypoint.clearBody
import app.fluffy.patches.waypoint.ensureRegisters
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val randomDeviceIdPatch = bytecodePatch(
    name = "Random device ID",
    description = "Reports a fresh random device ID (UUID) on every activation request " +
        "instead of the ID the app generated and stored at first launch. omarea's " +
        "activation server sees a new device each time.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SCENE)

    execute {
        // The device-ID getter is located by fingerprint (obfuscated holder class
        // renamed per build); toString() - what activation requests carry -
        // delegates to it.
        DeviceIdGetterFingerprint.method.apply {
            clearBody()
            ensureRegisters(2)
            addInstructions(
                0,
                """
                    invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;
                    move-result-object v0
                    invoke-virtual {v0}, Ljava/util/UUID;->toString()Ljava/lang/String;
                    move-result-object v0
                    return-object v0
                """,
            )
        }
    }
}
