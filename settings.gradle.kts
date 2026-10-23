pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository { maven("https://maven.ghostscript.com") }
            filter { includeGroup("com.artifex.mupdf") }
        }
    }
}
rootProject.name = "Portylus"
include(":app")
