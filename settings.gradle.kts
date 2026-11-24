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
    // PREFER_SETTINGS：忽略 init 脚本 / 子项目注入的仓库（本机全局镜像配置会注入），
    // 统一使用下面声明的仓库，避免 FAIL_ON_PROJECT_REPOS 直接构建失败。
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CFServerMonitor"
include(":app")
