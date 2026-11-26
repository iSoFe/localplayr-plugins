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
  }
}

rootProject.name = "localplayr-plugins"

include(":host-api")

// Every folder under plugins/ that has a build.gradle.kts is a plugin module.
rootDir.resolve("plugins").listFiles()
  ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
  ?.sortedBy { it.name }
  ?.forEach { include(":plugins:${it.name}") }
