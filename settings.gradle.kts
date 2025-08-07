/*
 * Copyright (c) 2023-2024. [DarkCube]
 * All rights reserved.
 * You may not use or redistribute this software or any associated files without permission.
 * The above copyright notice shall be included in all copies of this software.
 */

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://nexus.darkcube.eu/repository/darkcube-group/") {
            name = "DarkCube"
            credentials(PasswordCredentials::class)
        }
    }
}

plugins {
    id("eu.darkcube.darkcube.settings") version "1.9.0"
}

rootProject.name = "BanSystem"
