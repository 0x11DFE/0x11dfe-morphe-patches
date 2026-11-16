package app.fluffy.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_WAYPOINT = Compatibility(
        name = "Waypoint",
        packageName = "com.greycastel.waypoint",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x4C8BF5,
        targets = listOf(
            AppTarget(
                version = "1.10.1",
            ),
        ),
    )
}
