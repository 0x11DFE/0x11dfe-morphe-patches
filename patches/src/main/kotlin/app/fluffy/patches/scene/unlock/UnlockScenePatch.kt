package app.fluffy.patches.scene.unlock

import app.fluffy.patches.shared.Constants.COMPATIBILITY_SCENE
import app.fluffy.patches.waypoint.clearBody
import app.fluffy.patches.waypoint.ensureRegisters
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockSceneProPatch = bytecodePatch(
    name = "Unlock Scene N1",
    description = "Unlocks Scene N1: the license state parser is forced to a perpetual " +
        "success (expiry 3089-12-31), the activation code type is reported as perpetual, " +
        "and the APK signature MD5 the app reports to omarea's activation binding is " +
        "spoofed to the original signing key so re-signed builds still match.",
) {
    compatibleWith(COMPATIBILITY_SCENE)

    execute {
        // La/c3;->b(String) ActivatedStateModel parses the stored license state
        // ("success@<millis>" / "expired" / ...). Replace the state source
        // (this.b.toString(), instructions 0-2) with a constant so every call
        // yields activated=true with the far-future expiry.
        mutableClassDefBy("La/c3;").methods.first {
            it.name == "b" &&
                it.parameterTypes == listOf("Ljava/lang/String;") &&
                it.returnType == "Lcom/omarea/model/ActivatedStateModel;"
        }.apply {
            removeInstructions(0, 3)
            addInstructions(0, "const-string v0, \"success@35343792000000\"")
        }

        // ActivationCodeResponse.getType() decides which license type gets stored
        // by the activation flow ("perpetual" / "trial" / "account" / ...).
        mutableClassDefBy("Lcom/omarea/model/ActivationCodeResponse;").methods.first {
            it.name == "getType" && it.parameterTypes.isEmpty() &&
                it.returnType == "Ljava/lang/String;"
        }.apply {
            clearBody()
            ensureRegisters(2)
            addInstructions(0, "const-string v0, \"perpetual\"\nreturn-object v0")
        }

        // Signature MD5 of the APK cert (activation binding). The helper class
        // was renamed between builds: at1.C (2026.10) -> rv1.B (2026.11).
        val signatureMethod = listOf("La/at1;" to "C", "La/rv1;" to "B")
            .firstNotNullOfOrNull { (className, methodName) ->
                runCatching { mutableClassDefBy(className) }.getOrNull()?.methods?.firstOrNull {
                    it.name == methodName &&
                        it.parameterTypes == listOf("Landroid/content/Context;", "Ljava/lang/String;") &&
                        it.returnType == "Ljava/lang/String;"
                }
            } ?: throw IllegalStateException(
                "Scene signature MD5 method not found (tried at1.C and rv1.B)"
            )
        signatureMethod.apply {
            clearBody()
            ensureRegisters(2)
            addInstructions(0, "const-string v0, \"34ea0e93b18c7df94decd663857bb3bf\"\nreturn-object v0")
        }
    }
}
