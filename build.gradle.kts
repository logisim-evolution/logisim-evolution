/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

import org.gradle.internal.os.OperatingSystem
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.collections.ArrayList

plugins {
  checkstyle
  id("com.github.ben-manes.versions") version "0.38.0"
  java
  application
  id("com.github.johnrengelman.shadow") version "7.0.0"
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
  implementation("org.drjekyll:fontchooser:2.4")
  implementation("at.swimmesberger:swingx-core:1.6.8")
  implementation("org.scijava:swing-checkbox-tree:1.0.2")
  implementation("org.slf4j:slf4j-api:1.7.30")
  implementation("org.slf4j:slf4j-simple:1.7.30")
  implementation("com.formdev:flatlaf:1.2")

  // NOTE: Be aware of reported issues with Eclipse and Batik
  // See: https://github.com/logisim-evolution/logisim-evolution/issues/709
  // implementation("org.apache.xmlgraphics:batik-swing:1.14")

  testImplementation("org.junit.vintage:junit-vintage-engine:5.7.1")
}

java {
  sourceCompatibility = JavaVersion.VERSION_14
  targetCompatibility = JavaVersion.VERSION_14
}

java {
  sourceSets["main"].java {
    srcDir("${buildDir}/generated/sources/srcgen")
    srcDir("${buildDir}/generated/logisim/")
  }
}

task<Jar>("sourcesJar") {
  group = "build"
  description = "Creates a source JAR archive."
  dependsOn.add("classes")
  classifier = "src"

  from(sourceSets.main.get().allSource)
}

extra.apply {
  val suffix: String by project
  val version = if (suffix != "") "${project.version}-${suffix}" else "${project.version}"
  val baseFilename = "${project.name}-${version}"
  set("targetFilePathBase", "${buildDir}/dist/${baseFilename}")

  val javaHome = System.getProperty("java.home") ?: throw GradleException("java.home is not set")
  val jpackage = javaHome + File.separator + "bin" + File.separator + "jpackage"
  val jPackageCmd = if (jpackage.contains(" ")) "\"" + jpackage + "\"" else jpackage
  set("jPackageCmd", jPackageCmd)
  val parameters = ArrayList<String>(listOf(
      jPackageCmd,
      "--input", "${buildDir}/libs",
      "--main-class", "com.cburch.logisim.Main",
      "--main-jar", "${baseFilename}-all.jar",
      "--app-version", version,
      "--copyright", "Copyright ©2001–${SimpleDateFormat("yyyy").format(Date())} ${project.name} developers",
      "--dest", "${buildDir}/dist"
  ))
  val linuxParameters = ArrayList<String>(listOf(
      "--name", project.name,
      "--file-associations", "${projectDir}/support/jpackage/linux/file.jpackage",
      "--icon", "${projectDir}/support/jpackage/linux/logisim-icon-128.png",
      "--install-dir", "/opt",
      "--linux-shortcut"
  ))
  set("sharedParameters", parameters)
  set("linuxParameters", linuxParameters)

  // macOS related stuff
  val uppercaseProjectName = project.name.capitalize().trim()
  set("uppercaseProjectName", uppercaseProjectName)
  set("appDirName", "${buildDir}/dist/${uppercaseProjectName}.app")
}

tasks.register("createDistDir") {
  group = "build"
  description = "Creates the directory for distribution."
  dependsOn("shadowJar")
  inputs.dir("${buildDir}/libs")
  outputs.dir("${buildDir}/dist")
  doLast {
    if (File("${buildDir}/libs").list().count() != 1) {
      throw GradleException("${buildDir}/libs should just contain a single shadowJar file. Try \"./gradlew clean\" first.")
    }
    val folder = File("${buildDir}/dist")
    if (!folder.exists() && !folder.mkdirs()) {
      throw GradleException("Unable to create directory \"${buildDir}/dist\".")
    }
  }
}

/*
 * Task: createDeb
 *
 * Creates the Linux DEB package file (Debian, Ubuntu and derrivatives).
 */
tasks.register("createDeb") {
  group = "build"
  description = "Makes DEB Linux installation package."
  dependsOn("shadowJar", "createDistDir")
  inputs.dir("${buildDir}/libs")
  inputs.dir("${projectDir}/support/jpackage/linux")
  outputs.file(ext.get("targetFilePathBase") as String + "-1_amd64.deb")
  doLast {
    if (!OperatingSystem.current().isLinux) {
      throw GradleException("This task runs on Linux only.")
    }

    val parameters = ArrayList<String>(ext.get("sharedParameters") as ArrayList<String>)
    parameters.addAll(ext.get("linuxParameters") as ArrayList<String>)
    val processBuilder1 = ProcessBuilder()
    processBuilder1.command(parameters)
    println()
println(processBuilder1.command().joinToString(" "))
    println()
    val process1 = processBuilder1.start()
    if (process1.waitFor() != 0) {
      throw GradleException("Error while creating the .deb package")
    }
  }
}

/*
 * Task: createRpm
 *
 * Creates the Linux RPM package file (RedHat and derrivatives).
 */
tasks.register("createRpm") {
  group = "build"
  description = "Makes RPM Linux installation package."
  dependsOn("shadowJar", "createDistDir")
  inputs.dir("${buildDir}/libs")
  inputs.dir("${projectDir}/support/jpackage/linux")
  outputs.file(ext.get("targetFilePathBase") as String + "-1.x86_64.rpm")
  doLast {
    if (!OperatingSystem.current().isLinux) {
      throw GradleException("This task runs on Linux only.")
    }

    val parameters = ArrayList<String>(ext.get("sharedParameters") as ArrayList<String>)
    parameters.addAll(ext.get("linuxParameters") as ArrayList<String>)
    parameters.addAll(listOf(
        "--type", "rpm"
    ))
    val processBuilder2 = ProcessBuilder()
    processBuilder2.command(parameters)
    val process2 = processBuilder2.start()
    if (process2.waitFor() != 0) {
      throw GradleException("Error while creating the .rpm package")
    }
  }
}

/*
 * Task: createMsi
 *
 * Creates MSI installater file for Microsoft Windows.
 */
tasks.register("createMsi") {
  group = "build"
  description = "Makes the Windows installation package."
  dependsOn("shadowJar", "createDistDir")
  inputs.dir("${buildDir}/libs")
  inputs.dir("${projectDir}/support/jpackage/windows")
  outputs.file(ext.get("targetFilePathBase") as String + ".msi")
  doLast {
    if (!OperatingSystem.current().isWindows) {
      throw GradleException("This task runs on Windows only.")
    }

    val parameters = ArrayList<String>(ext.get("sharedParameters") as ArrayList<String>)
    parameters.addAll(listOf(
        "--name", project.name,
        "--file-associations", "${projectDir}/support/jpackage/windows/file.jpackage",
        "--icon", "${projectDir}/support/jpackage/windows/Logisim-evolution.ico",
        "--type", "msi",
        "--win-menu-group", "logisim",
        "--win-shortcut",
        "--win-dir-chooser",
        "--win-menu"
    ))
    val processBuilder1 = ProcessBuilder()
    processBuilder1.command(parameters)
    val process1 = processBuilder1.start()
    if (process1.waitFor() != 0) {
      throw GradleException("Error while creating the MSI package.")
    }
  }
}

/*
 * Task: createApp
 *
 * Creates macOS application.
 */
tasks.register("createApp") {
  group = "build"
  description = "Makes the macOS application."
  dependsOn("shadowJar", "createDistDir")
  inputs.dir("${buildDir}/libs")
  inputs.dir("${projectDir}/support/jpackage/macos")
  outputs.dir(ext.get("appDirName") as String)
  doLast {
    if (!OperatingSystem.current().isMacOsX) {
      throw GradleException("This task runs on macOS only.")
    }

    val appDirName = ext.get("appDirName") as String
    delete(appDirName)
    val parameters = ArrayList<String>(ext.get("sharedParameters") as ArrayList<String>)
    parameters.addAll(listOf(
        "--name", ext.get("uppercaseProjectName") as String,
        "--file-associations", "${projectDir}/support/jpackage/macos/file.jpackage",
        "--icon", "${projectDir}/support/jpackage/macos/Logisim-evolution.icns",
        "--type", "app-image"
    ))
    val processBuilder1 = ProcessBuilder()
    processBuilder1.command(parameters)
    val process1 = processBuilder1.start()
    if (process1.waitFor() != 0) {
      throw GradleException("Error while creating the .app directory.")
    }
    val pListFilename = "${appDirName}/Contents/Info.plist"
    val parameters2 = ArrayList<String>(listOf(
        "awk",
        "/Unknown/{sub(/Unknown/,\"public.app-category.education\")};"
            + "{print >\"${buildDir}/dist/Info.plist\"};"
            + "/NSHighResolutionCapable/{"
                + "print \"  <string>true</string>\" >\"${buildDir}/dist/Info.plist\";"
                + "print \"  <key>NSSupportsAutomaticGraphicsSwitching</key>\" >\"${buildDir}/dist/Info.plist\""
            + "}",
        pListFilename
    ))
    val processBuilder2 = ProcessBuilder()
    processBuilder2.command(parameters2)
    val process2 = processBuilder2.start()
    if (process2.waitFor() != 0) {
      throw GradleException("Error while patching Info.plist file.")
    }
    val parameters3 = ArrayList<String>(listOf(
        "mv", "${buildDir}/dist/Info.plist", pListFilename
    ))
    val processBuilder3 = ProcessBuilder()
    processBuilder3.command(parameters3)
    val process3 = processBuilder3.start()
    if (process3.waitFor() != 0) {
      throw GradleException("Error while moving Info.plist into the .app directory.")
    }
    val parameters4 = ArrayList<String>(listOf(
        "codesign", "--remove-signature", appDirName
    ))
    val processBuilder4 = ProcessBuilder()
    processBuilder4.command(parameters4)
    val process4 = processBuilder4.start()
    if (process4.waitFor() != 0) {
      throw GradleException("Error while executing: codesign --remove-signature")
    }
  }
}

/*
 * Task: createDmg
 *
 * Creates macOS DMG package file.
 */
tasks.register("createDmg") {
  group = "build"
  description = "Makes the macOS DMG package."
  dependsOn("createApp")
  inputs.dir(ext.get("appDirName") as String)
  outputs.file(ext.get("targetFilePathBase") as String + ".dmg")
  doLast {
    val suffix: String by project
    val version = if (suffix != "") "${project.version}-${suffix}" else "${project.version}"

    if (OperatingSystem.current().isMacOsX) {
      val parameters1 = ArrayList<String>(listOf(
          ext.get("jPackageCmd") as String,
          "--type", "dmg",
          "--app-image", ext.get("appDirName") as String,
          "--name", project.name,
          "--app-version", version,
          "--dest", "${buildDir}/dist"
      ))
      val processBuilder1 = ProcessBuilder()
      processBuilder1.command(parameters1)
      val process1 = processBuilder1.start()
      if (process1.waitFor() != 0) {
        throw GradleException("Error while creating the DMG package")
      }
    }
  }
}

fun String.runCommand(workingDir: File = File("."), timeoutAmount: Long = 60, timeoutUnit: TimeUnit = TimeUnit.SECONDS):
        String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
  .directory(workingDir)
  .redirectOutput(ProcessBuilder.Redirect.PIPE)
  .redirectError(ProcessBuilder.Redirect.PIPE)
  .start()
  .apply { waitFor(timeoutAmount, timeoutUnit) }
  .run {
    val error = errorStream.bufferedReader().readText().trim()
    if (error.isNotEmpty()) {
      throw Exception(error)
    }
    inputStream.bufferedReader().readText().trim()
  }

/*
 * Task: generateProjectInfoClassFile
 *
 * Generates Java class file with project information like current version, branch name, last commit hash etc.
 * No need to trigger it manually.
 */
tasks.register("generateBuildInfoClassFile") {
  group = "build"
  description = "Creates Java class file with vital project information."

  // Target location for generated files.
  val projectInfoDir = "${buildDir}/generated/logisim/java/com/cburch/logisim/generated"
  // Full path to the Java class file to be generated.
  val projectInfoFile = "${projectInfoDir}/BuildInfo.java"

  // TODO: we should not have hardcoded path here but use default sourcesSet maybe?
  inputs.dir("$projectDir/src")
  inputs.dir("$projectDir/support")
  inputs.files("$projectDir/gradle.properties", "$projectDir/README.md", "$projectDir/LICENSE.md")
  outputs.dir(projectInfoDir)

  doLast {

    val now = Date()
    val branchName = "git rev-parse --abbrev-ref HEAD".runCommand(workingDir = rootDir)
    val branchLastCommitHash = "git rev-parse --short=8 HEAD".runCommand(workingDir = rootDir)
    val currentMillis = Date().time
    val buildYear = SimpleDateFormat("yyyy").format(now)

    // Project properties can be accessed via delegation
    val suffix: String by project
    val version = if (suffix != "") "${project.version}-${suffix}" else "${project.version}"

    var buildInfoClass = arrayOf(
      "// ************************************************************************",
      "// THIS IS COMPILE TIME GENERATED FILE! DO NOT EDIT BY HAND!",
      "// Use './gradlew generateBuildInfoClassFile' to regenerate if needed.",
      "// Generated at " + SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(now),
      "// ************************************************************************",
      "",
      "package com.cburch.logisim.generated;",
      "",
      "import com.cburch.logisim.LogisimVersion;",
      "import java.util.Date;",
      "",
      "public final class BuildInfo {",
      "    // Build time VCS details",
      "    public static final String branchName = \"${branchName}\";",
      "    public static final String branchLastCommitHash = \"${branchLastCommitHash}\";",
      "    public static final String buildId = \"${branchName}/${branchLastCommitHash}\";",
      "",
      "    // Project build timestamp",
      "    public static final long millis = ${currentMillis}L;",
      "    public static final String year = \"${buildYear}\";",
      "    public static final Date date = new Date();",
      "    static { date.setTime(millis); }",
      "",
      "    // Project version",
      "    public static final LogisimVersion version = LogisimVersion.fromString(\"${version}\");",
      "    public static final String name = \"${project.name.capitalize().trim()}\";",
      "}",
    )

    file(projectInfoDir).mkdirs()
    file(projectInfoFile).writeText(buildInfoClass.joinToString("\n"))
  }
}

/*
 * Task: jpackage
 *
 * Umbrella task to create packages for all supported platforms.
 */
tasks.register("jpackage") {
  group = "build"
  description = "Makes the platform specific packages for the current platform."
  dependsOn("createDeb", "createRpm", "createMsi", "createDmg")
}

tasks {
  compileJava {
    options.compilerArgs = listOf("-Xlint:deprecation", "-Xlint:unchecked", "-Xlint:fallthrough")
    dependsOn("generateBuildInfoClassFile")
  }
  compileTestJava {
    options.compilerArgs = listOf("-Xlint:deprecation", "-Xlint:unchecked", "-Xlint:fallthrough")
    dependsOn("generateBuildInfoClassFile")
  }
  jar {
    manifest {
      attributes.putAll(mapOf(
          "Implementation-Title" to name,
          "Implementation-Version" to archiveVersion
      ))
    }

    from(".") {
      include("LICENSE")
      include("README.md")
    }
  }
  shadowJar {
    val suffix: String by project
    val version = if (suffix != "") "${project.version}-${suffix}" else "${project.version}"
    val baseFilename = "${project.name}-${version}"
//    set("targetFilePathBase", "${buildDir}/dist/${baseFilename}")
//    archiveBaseName.set(ext.get("targetFilePathBase") as String)
    archiveBaseName.set(project.name)
    archiveVersion.set(version)
    from(".") {
      include("LICENSE")
      include("README.md")
    }
  }

  // Checkstyles related tasks: "checkstylMain" and "checkstyleTest"
  checkstyle {
    // Checkstyle version to use
    toolVersion = "8.43"

    // let's use google_checks.xml config provided with Checkstyle.
    // https://stackoverflow.com/a/67513272/1235698
    val archive = configurations.checkstyle.get().resolve().filter {
      it.name.startsWith("checkstyle")
    }
    config = resources.text.fromArchiveEntry(archive, "google_checks.xml")

    // FIXME there should be cleaner way of using custom suppression config with built-in style
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
