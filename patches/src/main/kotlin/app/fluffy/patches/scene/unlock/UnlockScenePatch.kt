package app.fluffy.patches.scene.unlock

import app.fluffy.patches.shared.Constants.COMPATIBILITY_SCENE
import app.fluffy.patches.waypoint.clearBody
import app.fluffy.patches.waypoint.ensureRegisters
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.time.LocalDate
import java.time.ZoneOffset

@Suppress("unused")
val unlockSceneProPatch = bytecodePatch(
    name = "Unlock Scene N1",
    description = "Unlocks Scene N1: the license state parser is forced to a perpetual " +
        "success (configurable expiry), the activation code type is reported as perpetual, " +
        "and the APK signature MD5 the app reports to omarea's activation binding is " +
        "spoofed to the original signing key so re-signed builds still match. Optional: " +
        "report a fresh random device ID instead of the stored one.",
) {
    compatibleWith(COMPATIBILITY_SCENE)

    val expiryDate by stringOption(
        key = "expiry-date",
        default = "2077-01-01",
        values = mapOf(
            "2077-01-01" to "2077 (default)",
            "3089-12-31" to "Far future",
            "2100-01-01" to "Year 2100",
            "2030-01-01" to "Year 2030",
        ),
        title = "License expiry date",
        description = "Date the spoofed Scene N1 license expires, as yyyy-MM-dd. " +
            "The app shows it on the user page; anything in the future keeps Pro active.",
        required = true,
        validator = { it != null && runCatching { LocalDate.parse(it) }.isSuccess },
    )

    val randomDeviceId by booleanOption(
        key = "random-device-id",
        default = false,
        title = "Random device ID",
        description = "Report a fresh random device ID on every request instead of the " +
            "ID stored at first launch. The activation server sees a new device each time.",
    )

    execute {
        val millis = try {
            LocalDate.parse(expiryDate)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli().toString()
        } catch (e: Exception) {
            throw PatchException("Invalid expiry-date '$expiryDate' (expected yyyy-MM-dd): ${e.message}")
        }

        // La/c3;->b(String) ActivatedStateModel parses the stored license state
        // ("success@<millis>" / "expired" / ...). Replace the state source
        // (this.b.toString(), instructions 0-2) with a constant so every call
        // yields activated=true with the configured expiry.
        mutableClassDefBy("La/c3;").methods.first {
            it.name == "b" &&
                it.parameterTypes == listOf("Ljava/lang/String;") &&
                it.returnType == "Lcom/omarea/model/ActivatedStateModel;"
        }.apply {
            removeInstructions(0, 3)
            addInstructions(0, "const-string v0, \"success@$millis\"")
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
        // was renamed between builds: at1.C (2026.10) -> rv1.B (2026.11 PR1) ->
        // mw1.B (2026.11 PR2).
        val signatureMethod = listOf("La/at1;" to "C", "La/rv1;" to "B", "La/mw1;" to "B")
            .firstNotNullOfOrNull { (className, methodName) ->
                runCatching { mutableClassDefBy(className) }.getOrNull()?.methods?.firstOrNull {
                    it.name == methodName &&
                        it.parameterTypes == listOf("Landroid/content/Context;", "Ljava/lang/String;") &&
                        it.returnType == "Ljava/lang/String;"
                }
            } ?: throw PatchException(
                "Scene signature MD5 method not found (tried at1.C, rv1.B, mw1.B)"
            )
        signatureMethod.apply {
            clearBody()
            ensureRegisters(2)
            addInstructions(0, "const-string v0, \"34ea0e93b18c7df94decd663857bb3bf\"\nreturn-object v0")
        }

        // Optional: fresh random device ID. The holder class that owns the stored
        // ID was renamed between builds: at (2026.10) -> tt (2026.11 PR1) -> nu
        // (2026.11 PR2); its a()Ljava/lang/String; reads/caches the processed ID
        // and toString() (what the activation requests carry) delegates to it.
        if (randomDeviceId == true) {
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
}
