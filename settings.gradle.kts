pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // Mapbox Maven repository
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                // Read from local.properties
                val localProperties = java.util.Properties()
                val localPropertiesFile = File(rootDir, "local.properties")
                if (localPropertiesFile.exists()) {
                    localPropertiesFile.inputStream().use { localProperties.load(it) }
                }
                password = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN")
                    ?: providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").orNull
                    ?: System.getenv("MAPBOX_DOWNLOADS_TOKEN")
                    ?: ""
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        
        maven {
            url = uri("https://jitpack.io")
        }
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}

rootProject.name = "Fonts Reviewer"
include(":app")
