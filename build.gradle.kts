gradle.taskGraph.whenReady {
    allTasks.filterIsInstance<JavaExec>().forEach {
        it.setExecutable(it.javaLauncher.get().executablePath.asFile.absolutePath)
    }
}

plugins {
    java
    alias(libs.plugins.shadow)
    id("eu.darkcube.darkcube")
}

tasks {
    jar {
        destinationDirectory = temporaryDir
    }
    shadowJar {
        archiveClassifier = null
        relocate("eu.darkcube.discordbot", "eu.darkcube.bansystem.libs.discordbot")
    }
    assemble {
        dependsOn(shadowJar)
    }
}

dependencies {
    annotationProcessor(libs.velocity)
    compileOnly(libs.velocity)
    compileOnly(libs.paper.api)
    compileOnly(libs.darkcubesystem.api)
    compileOnly(libs.cloudnet.bridge)
    compileOnly(libs.cloudnet.driver)
    implementation(libs.discordbot.api)
}