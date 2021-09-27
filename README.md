# GTBlocks

This is a sample, downscaled GTBlocks to simplify reproducing the [issue #1353](https://github.com/JetBrains/Exposed/issues/1353). All the dependencies related to Minecraft server are already included on this project.

## Steps to reproduce issues

### Before All
* Set project Java version to `Java 8` (and run everything using it too)

### java.lang.IllegalStateException: Can't load implementation for DatabaseConnectionAutoRegistration
1. Uncomment line `relocate("org.jetbrains.exposed", "${dependencyPackage}.exposed")` from `build.gradle.kts`, make sure Kotlin relocation is disabled (`relocate("kotlin", "${dependencyPackage}.kotlin")`).
2. Build the project by running Gradle `build` task
3. Run `Server/paper-1.12.2-1618.jar` through the IDE by adding it as JAR Application, or use the included `Server/run.bat` to run the Minecraft server jar. Keep in mind that the batch file uses your JAVA_HOME, and this server version uses Java 8, so you might have to edit it before you run.
4. Done, the issue will pop up in the console as soon as the plugin `GTBlocks` is loaded.

### java.lang.AssertionError: Built-in class com.github.secretx33.dependencies.gtblocks.kotlin.Any is not found
1. Uncomment line `relocate("kotlin", "${dependencyPackage}.kotlin")` from `build.gradle.kts`, make sure Exposed relocation is disabled (`relocate("org.jetbrains.exposed", "${dependencyPackage}.exposed")`).
2. Build the project by running Gradle `build` task
3. Run `Server/paper-1.12.2-1618.jar` through the IDE by adding it as JAR Application, or use the included `Server/run.bat` to run the Minecraft server jar. Keep in mind that the batch file uses your JAVA_HOME, and this server version uses Java 8, so you might have to edit it before you run.
4. Done, the issue will pop up in the console as soon as the plugin `GTBlocks` is loaded.
