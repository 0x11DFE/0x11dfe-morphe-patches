package app.fluffy.patches.scene.privacy

import app.fluffy.patches.shared.Constants.COMPATIBILITY_SCENE
import app.fluffy.patches.waypoint.clearBody
import app.fluffy.patches.waypoint.ensureRegisters
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
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
        // The holder class that owns the stored ID was renamed between builds:
        // at (2026.10) -> tt (2026.11 PR1) -> nu (2026.11 PR2). Its
        // a()Ljava/lang/String; reads/caches the processed ID and toString()
        // (what the activation requests carry) delegates to it.
        val deviceIdMethod = listOf("La/at;", "La/tt;", "La/nu;")
            .firstNotNullOfOrNull { className ->
                runCatching { mutableClassDefBy(className) }.getOrNull()?.methods?.firstOrNull {
                    it.name == "a" && it.parameterTypes.isEmpty() &&
                        it.returnType == "Ljava/lang/String;"
                }
            } ?: throw PatchException(
                "Scene device ID getter not found (tried at.a, tt.a, nu.a)"
            )
        deviceIdMethod.apply {
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
