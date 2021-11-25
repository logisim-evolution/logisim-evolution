/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

import org.gradle.internal.os.OperatingSystem
import java.text.SimpleDateFormat
import java.util.Date
import java.io.*

plugins {
  checkstyle
  id("com.github.ben-manes.versions") version "0.38.0"
  java
  application
  id("com.github.johnrengelman.shadow") version "7.0.0"
  id("org.sonarqube") version "3.3"
}

repositories {
  mavenCentral()
}

application {
  mainClass.set("com.cburch.logisim.Main")
}

dependencies {
  implementation("org.hamcrest:hamcrest:2.2")
  implementation("javax.help:javahelp:2.0.05")
  implementation("com.fifesoft:rsyntaxtextarea:3.1.2")
  implementation("net.sf.nimrod:nimrod-laf:1.2")
  implementation("org.drjekyll:colorpicker:1.3")
  implementation("at.swimmesberger:swingx-core:1.6.8")
  implementation("org.scijava:swing-checkbox-tree:1.0.2")
  implementation("org.slf4j:slf4j-api:1.7.30")
  implementation("org.slf4j:slf4j-simple:1.7.30")
  implementation("com.formdev:flatlaf:1.2")
  implementation("commons-cli:commons-cli:1.4")
  implementation("org.apache.commons:commons-text:1.9")

  compileOnly("org.jetbrains:annotations:22.0.0")

  // NOTE: Be aware of reported issues with Eclipse and Batik
  // See: https://github.com/logisim-evolution/logisim-evolution/issues/709
  // implementation("org.apache.xmlgraphics:batik-swing:1.14")

  testImplementation(platform("org.junit:junit-bom:5.8.1"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.mockito:mockito-inline:4.0.0")
  testImplementation("org.mockito:mockito-junit-jupiter:4.0.0")
}

sonarqube {
  properties {
    property("sonar.projectKey", "logisim-evolution_logisim-evolution")
    property("sonar.organization", "logisim-evolution")
    property("sonar.host.url", "https://sonarcloud.io")
  }
}

/**
 * Strings used as keys to reference shared variables (via `ext.*`)
 */
val APP_DIR_NAME = "appDirName"
val APP_VERSION = "appVersion"
val APP_VERSION_SHORT = "appVersionShort"
val APP_URL = "appUrl"
val JPACKAGE = "jpackage"
val LIBS_DIR = "libsDir"
val LINUX_PARAMS = "linuxParameters"
val SHADOW_JAR_FILE_NAME = "shadowJarFilename"
val SHARED_PARAMS = "sharedParameters"
val SUPPORT_DIR = "supportDir"
val TARGET_DIR = "targetDir"
val TARGET_FILE_PATH_BASE = "targetFilePathBase"
val TARGET_FILE_PATH_BASE_SHORT = "targetFilePathBaseShort"
val UPPERCASE_PROJECT_NAME = "uppercaseProjectName"

java {
  sourceCompatibility = JavaVersion.VERSION_16
  targetCompatibility = JavaVersion.VERSION_16
}

/**
 * Setting up all shared vars and parameters.
 */
extra.apply {
  // NOTE: optional suffix is prefixed with `-` (because of how LogisimVersion class parses it), which
  // I remove here because `jpackage` tool does not like it when used to build the RPM package.
  // Do NOT use `project.version` instead.
  val appVersion = (project.version as String).replace("-", "")
  set(APP_VERSION, appVersion)
  logger.info("appVersion: ${appVersion}")

  val appUrl = findProperty("url")
  set(APP_URL, appUrl)
  logger.info("appUrl: ${appUrl}")

  // Short (with suffix removed) version string, i.e. for "3.6.0beta1", short form is "3.6.0".
  // This is used by createApp and createMsi as version numbering is pretty strict on macOS and Windows.
  // Do NOT use `project.version` instead.
  val appVersionShort = (project.version as String).split('-')[0]
  set(APP_VERSION_SHORT, appVersionShort)
  logger.info("appVersionShort: ${appVersionShort}")

  // Destination folder where packages are stored.
  val targetDir="${buildDir}/dist"
  set(TARGET_DIR, targetDir)

  // JAR folder.
  val libsDir="${buildDir}/libs"
  set(LIBS_DIR, libsDir)

  // The root dir for jpackage extra files.
  val supportDir="${projectDir}/support/jpackage"
  set(SUPPORT_DIR, supportDir)

  // Base name of produced artifacts. Suffixes will be added later by relevant tasks.
  val baseFilename = "${project.name}-${appVersion}"
  set(TARGET_FILE_PATH_BASE, "${targetDir}/${baseFilename}")
  logger.debug("targetFilePathBase: \"${targetDir}/${baseFilename}\"")

  val baseFilenameShort = "${project.name}-${appVersionShort}"
  set(TARGET_FILE_PATH_BASE_SHORT, "${targetDir}/${baseFilenameShort}")
  logger.debug("targetFilePathBaseShort: \"${targetDir}/${baseFilenameShort}\"")

  // Name of application shadowJar file.
  val shadowJarFilename = "${baseFilename}-all.jar"
  set(SHADOW_JAR_FILE_NAME, shadowJarFilename)
  logger.debug("shadowJarFilename: \"${shadowJarFilename}\"")

  // JDK/jpackage vars
  val javaHome = System.getProperty("java.home") ?: throw GradleException("java.home is not set")
  val jpackage = "${javaHome}/bin/jpackage"
  set(JPACKAGE, jpackage)

  // Copyrights note.
  val copyrights = "Copyright ©2001–${SimpleDateFormat("yyyy").format(Date())} ${project.name} developers"

  // Platform-agnostic jpackage parameters shared across all the builds.
  var params = listOf(
      jpackage,
      // NOTE: we cannot use --app-version as part of platform agnostic set as i.e. both macOS and
      // Windows packages do not allow use of any suffixes like "-dev" etc, so --app-version is set
      // in these builders separately.
      "--input", libsDir,
      "--main-class", "com.cburch.logisim.Main",
      "--main-jar", shadowJarFilename,
      "--copyright", copyrights,
      "--dest", targetDir,
      "--description", "Digital logic design tool and simulator",
      "--vendor", "${project.name} developers",
  )
  if (logger.isDebugEnabled()) {
    params += listOf("--verbose")
  }
  set(SHARED_PARAMS, params)

  // Linux (DEB/RPM) specific settings for jpackage.
  val linuxParams = params + listOf(
      "--name", project.name,
      "--app-version", appVersion,
      "--file-associations", "${supportDir}/linux/file.jpackage",
      "--icon", "${supportDir}/linux/logisim-icon-128.png",
      "--install-dir", "/opt",
      "--linux-shortcut"
  )
  set(LINUX_PARAMS, linuxParams)

  // All the macOS specific stuff.
  val uppercaseProjectName = project.name.capitalize().trim()
  set(UPPERCASE_PROJECT_NAME, uppercaseProjectName)
  set(APP_DIR_NAME, "${targetDir}/${uppercaseProjectName}.app")
}

java {
  sourceSets["main"].java {
    srcDir("${buildDir}/generated/logisim/java")
    srcDir("${buildDir}/generated/sources/srcgen")
  }
}

task<Jar>("sourcesJar") {
  group = "build"
  description = "Creates a JAR archive with project sources."
  dependsOn.add("classes")
  classifier = "src"

  from(sourceSets.main.get().allSource)
  archiveVersion.set(ext.get(APP_VERSION) as String)
}



/**
 * Creates the distribution directory and removes any extra files from the libs directory
 * beyond the expected shadowJar file because jpackage puts everything from libs into the package.
 */
tasks.register("createDistDir") {
  val libsDir = ext.get(LIBS_DIR) as String

  group = "build"
  description = "Creates the directory for distribution."
  dependsOn("shadowJar")

  val shadowJarFilename = ext.get(SHADOW_JAR_FILE_NAME) as String

  inputs.dir(libsDir)
  outputs.dir(ext.get(TARGET_DIR) as String)

  doFirst {
    var jarFiles = File(libsDir).list()
    var jarCount = jarFiles.count()

    if ( jarCount > 1) {
      logger.warn("Expected 1 shadowJar file, found ${jarCount} in: ${libsDir}")

      val expectedShadowJar = File("${libsDir}/${shadowJarFilename}")
      if (expectedShadowJar.exists()) {
        logger.warn("Found needed: ${shadowJarFilename}")
      }

      for (file in jarFiles.filter { file -> file != shadowJarFilename }) {
        logger.warn("Will remove: ${file}")
      }
    }
  }

  doLast {
    val folder = File(libsDir)
    if (!folder.exists() && !folder.mkdirs()) {
      throw GradleException("Unable to create directory: ${folder.absolutePath}")
    }

    var jarFiles = File(libsDir).list()
    var jarCount = jarFiles.count()
    if ( jarCount > 1) {
      // Value of shadowJarFileName is our needed JAR file. Remove the rest.
      for (file in jarFiles.filter { file -> file != shadowJarFilename }) {
        if (!delete("${libsDir}/${file}")) {
          throw GradleException("Failed to remove orphaned file: ${file}")
        }
      }
    }
  }
}

/**
 * Helper method that simplifies runining external commands using ProcessBuilder().
 * Will throw GradleException on command failure (non-zero return code).
 *
 * params: List of strings which signifies the external program file to be invoked and its arguments (if any).
 * exMsg: Optional error message to be used with thrown exception on failure.
 *
 * Returns content of invoked app's stdout
 */
fun runCommand(params: List<String>, exceptionMsg: String): String {
  val procBuilder = ProcessBuilder()
  procBuilder
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .command(params)
  val proc = procBuilder.start()

  logger.debug("EXECUTING CMD: " + params.joinToString(" "))

  var rc = -1
  try {
    rc = proc.waitFor()
    logger.debug("CMD COMPLETED. RC: ${rc}")
  } catch (ex: Exception) {
    logger.error(ex.message)
    logger.error(ex.stackTraceToString())
  }

  if (rc != 0) {
    logger.error(proc.errorStream.bufferedReader().readText().trim())
    logger.error("Command \"${params[0]}\" failed with RC ${rc}.")
    var exMsg = exceptionMsg ?: ""
    throw GradleException(exMsg)
  }

  return proc.inputStream.bufferedReader().readText().trim()
}

/**
 * Task: createDeb
 *
 * Creates the Linux DEB package file (Debian, Ubuntu and derrivatives).
 */
tasks.register("createDeb") {
  group = "build"
  description = "Makes DEB Linux installation package."
  dependsOn("shadowJar", "createDistDir")
  inputs.dir(ext.get(LIBS_DIR) as String)
  inputs.dir("${ext.get(SUPPORT_DIR) as String}/linux")

  // Debian uses `_` to separate name from version string.
  // https://www.debian.org/doc/manuals/debian-faq/pkg-basics.en.html
  val appVersion = ext.get(APP_VERSION) as String
  val targetDir = ext.get(TARGET_DIR) as String
  val debPackagePath = "${targetDir}/${project.name}_${appVersion}-1_amd64.deb"
  outputs.file(debPackagePath)

  doFirst {
    if (!OperatingSystem.current().isLinux) {
      throw GradleException("This task runs on Linux only.")
    }
  }

  doLast {
    val params = ext.get(LINUX_PARAMS) as List<String> + listOf("--type", "deb")
    runCommand(params, "Error while creating the DEB package.")
  }
}

/**
 * Task: createRpm
 *
 * Creates the Linux RPM package file (RedHat and derrivatives).
 */
tasks.register("createRpm") {
  group = "build"
  description = "Makes RPM Linux installation package."
  dependsOn("shadowJar", "createDistDir")
  inputs.dir(ext.get(LIBS_DIR) as String)
  inputs.dir("${ext.get(SUPPORT_DIR) as String}/linux")
  outputs.file("${ext.get(TARGET_FILE_PATH_BASE) as String}-1.x86_64.rpm")

  doFirst {
    if (!OperatingSystem.current().isLinux) {
      throw GradleException("This task runs on Linux only.")
    }
  }

  doLast {
    val params = ext.get(LINUX_PARAMS) as List<String> + listOf("--type", "rpm")
    runCommand(params, "Error while creating the RPM package.")
  }
}

/**
 * Task: createMsi
 *
 * Creates MSI installater file for Microsoft Windows.
 */
tasks.register("createMsi") {
  group = "build"
  description = "Makes the Windows installation package."
  dependsOn("shadowJar", "createDistDir")

  val supportDir = ext.get(SUPPORT_DIR) as String;

  inputs.dir(ext.get(LIBS_DIR) as String)
  inputs.dir("${supportDir}/windows")
  outputs.file("${ext.get(TARGET_FILE_PATH_BASE_SHORT) as String}.msi")

  doFirst {
    if (!OperatingSystem.current().isWindows) {
      throw GradleException("This task runs on Windows only.")
    }
  }

  doLast {
    val params = ext.get(SHARED_PARAMS) as List<String> + listOf(
        "--name", project.name,
        "--file-associations", "${supportDir}/windows/file.jpackage",
        "--icon", "${supportDir}/windows/Logisim-evolution.ico",
        "--win-menu-group", project.name as String,
        "--win-shortcut",
        "--win-dir-chooser",
        "--win-menu",
        "--type", "msi",
        // we MUST use short version form (without any suffix like "-dev", as it is not allowed in MSI package:
        // https://docs.microsoft.com/en-us/windows/win32/msi/productversion?redirectedfrom=MSDN
        // NOTE: any change to version **format** may require editing of .github/workflows/nightly.yml too!
        "--app-version", ext.get(APP_VERSION_SHORT) as String,
    )
    runCommand(params, "Error while creating the MSI package.")
  }
}

/**
 * Task: createApp
 *
 * Creates macOS application.
 */
tasks.register("createApp") {
  val supportDir = ext.get(SUPPORT_DIR) as String
  val appDirName = ext.get(APP_DIR_NAME) as String

  group = "build"
  description = "Makes the macOS application."
  dependsOn("shadowJar", "createDistDir")
  inputs.dir(ext.get(LIBS_DIR) as String)
  inputs.dir("${supportDir}/macos")
  outputs.dir(appDirName)

  doFirst {
    if (!OperatingSystem.current().isMacOsX) {
      throw GradleException("This task runs on macOS only.")
    }
  }

  doLast {
    delete(appDirName)

    var params = ext.get(SHARED_PARAMS) as List<String>
    params += listOf(
        "--name", ext.get(UPPERCASE_PROJECT_NAME) as String,
        "--file-associations", "${supportDir}/macos/file.jpackage",
        "--icon", "${supportDir}/macos/Logisim-evolution.icns",
        // app versioning is strictly checked for macOS. No suffix allowed for `app-image` type.
        "--app-version", ext.get(APP_VERSION_SHORT) as String,
        "--type", "app-image",
    )
    runCommand(params, "Error while creating the .app directory.")

    val targetDir = ext.get(TARGET_DIR) as String
    val pListFilename = "${appDirName}/Contents/Info.plist"
    runCommand(listOf(
        "awk",
        "/Unknown/{sub(/Unknown/,\"public.app-category.education\")};"
            + "/utilities/{sub(/public.app-category.utilities/,\"public.app-category.education\")};"
            + "{print >\"${targetDir}/Info.plist\"};"
            + "/NSHighResolutionCapable/{"
            + "print \"  <string>true</string>\" >\"${targetDir}/Info.plist\";"
            + "print \"  <key>NSSupportsAutomaticGraphicsSwitching</key>\" >\"${targetDir}/Info.plist\""
            + "}",
        pListFilename,
    ), "Error while patching Info.plist file.")

    runCommand(listOf(
        "mv", "${targetDir}/Info.plist", pListFilename
    ), "Error while moving Info.plist into the .app directory.")

    runCommand(listOf(
        "codesign", "--force", "--sign", "-", appDirName
    ), "Error while executing: codesign")
  }
}

/**
 * Task: createDmg
 *
 * Creates macOS DMG package file.
 */
tasks.register("createDmg") {
  group = "build"
  description = "Makes the macOS DMG package."
  dependsOn("createApp")

  val appDirName = ext.get(APP_DIR_NAME) as String

  inputs.dir(appDirName)
  outputs.file(ext.get(TARGET_FILE_PATH_BASE) as String + ".dmg")

  doFirst {
    if (!OperatingSystem.current().isMacOsX) {
      throw GradleException("This task runs on macOS only.")
    }
  }

  doLast {
    val params = listOf(
        ext.get(JPACKAGE) as String,
        "--app-image", appDirName,
        "--name", project.name,
        // We can pass full version here, even if contains suffix part too.
        "--app-version", ext.get(APP_VERSION) as String,
        "--dest", ext.get(TARGET_DIR) as String,
        "--type", "dmg",
      )
    runCommand(params, "Error while creating the DMG package")
  }
}

/**
 * Generates Java class file with project information like current version, branch name, last commit hash etc.
 */
fun genBuildInfo(buildInfoFilePath: String) {
  val now = Date()
  val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(now)

  var branchName = "";
  var branchLastCommitHash = "";
  var buildId = "(Not built from Git repo)";
  if (file("${projectDir}/.git").exists()) {
    var errMsg = "Failed getting branch name."
    branchName = runCommand(listOf("git", "-C", projectDir.toString(), "rev-parse", "--abbrev-ref", "HEAD"), errMsg)

    errMsg = "Failed getting last commit hash."
    branchLastCommitHash = runCommand(listOf("git", "-C", projectDir.toString(), "rev-parse", "--short=8", "HEAD"), errMsg)
    buildId = "${branchName}/${branchLastCommitHash}";
  }
  val currentMillis = Date().time
  val buildYear = SimpleDateFormat("yyyy").format(now)
  val appVersion = ext.get(APP_VERSION) as String
  val projectName = project.name.capitalize().trim()
  val displayName = "${projectName} v${appVersion}"
  val url = ext.get(APP_URL) as String

  val buildInfoClass = """
      // ************************************************************************
      // THIS IS A COMPILE TIME GENERATED FILE! DO NOT EDIT BY HAND!
      // Generated at ${nowIso}
      // ************************************************************************

      package com.cburch.logisim.generated;

      import com.cburch.logisim.LogisimVersion;
      import java.util.Date;

      public final class BuildInfo {
        // Build time VCS details
        public static final String branchName = "${branchName}";
        public static final String branchLastCommitHash = "${branchLastCommitHash}";
        public static final String buildId = "${buildId}";

        // Project build timestamp
        public static final long millis = ${currentMillis}L; // keep trailing 'L'
        public static final String year = "${buildYear}";
        public static final String dateIso8601 = "${nowIso}";
        public static final Date date = new Date();
        static { date.setTime(millis); }

        // Project version
        public static final LogisimVersion version = LogisimVersion.fromString("${appVersion}");
        public static final String name = "${projectName}";
        public static final String displayName = "${displayName}";
        public static final String url = "${url}";

        // JRE info
        public static final String jvm_version =
            String.format("%s v%s", System.getProperty("java.vm.name"), System.getProperty("java.version"));
        public static final String jvm_vendor = System.getProperty("java.vendor");
      }
      // End of generated BuildInfo

      """

  logger.info("Generating: ${buildInfoFilePath}")
  val buildInfoFile = File(buildInfoFilePath)
  buildInfoFile.parentFile.mkdirs()
  file(buildInfoFilePath).writeText(buildInfoClass.trimIndent())
}

/**
 * Task: genBuildInfo
 *
 * Wrapper task for genBuildInfo() method generating BuildInfo class.
 * No need to trigger it manually.
 */
tasks.register("genBuildInfo") {
  // Target location for generated files.
  val buildInfoDir = "${buildDir}/generated/logisim/java/com/cburch/logisim/generated"

  group = "build"
  description = "Creates Java class file with vital project information."

  // TODO: we should not have hardcoded path here but use default sourcesSet maybe?
  inputs.dir("${projectDir}/src")
  inputs.dir(ext.get(SUPPORT_DIR) as String)
  inputs.files("${projectDir}/gradle.properties", "${projectDir}/README.md", "${projectDir}/LICENSE.md")
  outputs.dir(buildInfoDir)

  doLast {
    // Full path to the Java class file to be generated.
    genBuildInfo("${buildInfoDir}/BuildInfo.java")
  }
}

/**
 * Task: genFiles
 *
 * Umbrella task to generate all generated files
*/
tasks.register("genFiles") {
  group = "build"
  description = "Generates all generated files."
  dependsOn("genBuildInfo")
}

/**
 * Task: createAll
 *
 * Umbrella task to create all packages for the current platform.
 */
tasks.register("createAll") {
  group = "build"
  description = "Makes the platform specific packages for the current platform."

  if (OperatingSystem.current().isLinux) {
    dependsOn("createDeb", "createRpm")
  }
  if (OperatingSystem.current().isWindows) {
    dependsOn("createMsi")
  }
  if (OperatingSystem.current().isMacOsX) {
    dependsOn("createDmg")
  }
}

/**
 * @deprecated. Use `createAll()`
 */
tasks.register("jpackage") {
  group = "build"
  var desc = "DEPRECATED: Use `createAll` task instead."
  description = desc
  dependsOn("createAll")

  doFirst {
    logger.warn(desc)
  }
}

val compilerOptions = listOf("-Xlint:deprecation", "-Xlint:unchecked")

tasks {
  compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs = compilerOptions
    dependsOn("genFiles")
  }
  compileTestJava {
    options.encoding = "UTF-8"
    options.compilerArgs = compilerOptions
    dependsOn("genFiles")
  }

  test {
    useJUnitPlatform()
//    testLogging {
//      events("passed", "skipped", "failed")
//    }
  }

  jar {
    manifest {
      attributes.putAll(mapOf(
          "Implementation-Title" to name,
          "Implementation-Version" to archiveVersion
      ))
    }

    from(".") {
      include("LICENSE.md")
      include("README.md")
      include("CHANGES.md")
    }
  }

  shadowJar {
    archiveBaseName.set(project.name)
    archiveVersion.set(ext.get(APP_VERSION) as String)
    from(".") {
      include("LICENSE.md")
      include("README.md")
      include("CHANGES.md")
    }
  }

  // Checkstyles related tasks: "checkstylMain" and "checkstyleTest"
  checkstyle {
    // Checkstyle version to use
    toolVersion = "8.45"

    // let's use google_checks.xml config provided with Checkstyle.
    // https://stackoverflow.com/a/67513272/1235698
    val archive = configurations.checkstyle.get().resolve().filter {
      name.startsWith("checkstyle")
    }
    config = resources.text.fromArchiveEntry(archive, "google_checks.xml")

    // FIXME: There should be cleaner way of using custom suppression config with built-in style.
    // https://stackoverflow.com/a/64703619/1235698
    System.setProperty( "org.checkstyle.google.suppressionfilter.config", "${projectDir}/config/checkstyle/suppressions.xml")
  }
  checkstyleMain {
    source = fileTree("src/main/java")
  }
  checkstyleTest {
    source = fileTree("src/test/java")
  }
}
