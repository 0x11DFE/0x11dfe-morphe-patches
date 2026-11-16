group = "app.fluffy"

patches {
    about {
        name = "Waypoint Morphe Patches"
        description = "Patches for Waypoint (com.greycastel.waypoint)"
        source = "local"
        author = "local"
        contact = "na"
        website = "https://morphe.software"
        license = "GPLv3"
    }
}

val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"
        dependsOn(build)
        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }
    publish {
        dependsOn("generatePatchesList")
    }
}
