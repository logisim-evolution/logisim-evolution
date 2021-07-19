import org.gradle.internal.os.OperatingSystem
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.mapOf

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

task<Jar>("sourcesJar") {
    group = "build"
    description = "Creates a source jar archive."
    dependsOn.add("classes")
    classifier = "src"

    from(sourceSets.main.get().allSource)
}

extra.apply {
   val df = SimpleDateFormat("yyyy")
   val year = df.format(Date())
   val javaHome = System.getProperty("java.home") ?: throw GradleException("java.home is not set")
   val cmd = javaHome + File.separator + "bin" + File.separator + "jpackage"
   val jPackageCmd = if (cmd.contains(" ")) "\"" + cmd + "\"" else cmd
   val parameters = ArrayList<String>(Arrays.asList(
      jPackageCmd,
      "--input", "$buildDir/libs",
      "--main-class", "com.cburch.logisim.Main",
      "--main-jar", project.name + '-' + project.version + "-all.jar",
      "--app-version", project.version as String,
      "--copyright", "Copyright © 2001–" + year + " Carl Burch, BFH, HEIG-VD, HEPIA, Holy Cross, et al.",
      "--dest", "$buildDir/dist"
   ))
   val linuxParameters = ArrayList<String>(Arrays.asList(
      "--name", project.name,
      "--file-associations", "$projectDir/support/jpackage/linux/file.jpackage",
      "--icon", "$projectDir/support/jpackage/linux/logisim-icon-128.png",
      "--install-dir", "/opt",
      "--linux-shortcut"
   ))
   set("sharedParameters", parameters)
   set("linuxParameters", linuxParameters)
   set("jPackageCmd", jPackageCmd)
   val projectName = project.name as String
   val projectVersion = project.version as String
   val uppercaseProjectName = projectName.substring(0,1).toUpperCase() + projectName.substring(1)
   set("uppercaseProjectName", uppercaseProjectName)
   set("appDirname", "$buildDir/dist/" + uppercaseProjectName + ".app")
   set("dmgFilename", "$buildDir/dist/" + projectName + "-" + projectVersion + ".dmg")
   set("rpmFilename", "$buildDir/dist/" + projectName + "-" + projectVersion + "-1.x86_64.rpm")
   set("debFilename", "$buildDir/dist/" + projectName + "_" + projectVersion + "-1_amd64.deb")
   set("msiFilename", "$buildDir/dist/" + projectName + "-" + projectVersion + ".msi")
}

tasks.register("createDistDir") {
   group = "build"
   description = "Creates the directory for distribution"
   dependsOn("shadowJar")
   inputs.dir("$buildDir/libs")
   outputs.dir("$buildDir/dist")
   doLast {
      if (File("$buildDir/libs").list().count() != 1) {
         throw GradleException("$buildDir/libs should just contain a single shadowjar file.")
      }
      val folder = File("$buildDir/dist")
      if (!folder.exists() && !folder.mkdirs()) {
         throw GradleException("Unable to create directory \"$buildDir/dist\"")
      }
   }
}

tasks.register("createDeb") {
   group = "build"
   description = "Makes the Linux platform specific packages"
   dependsOn("shadowJar", "createDistDir")
   inputs.dir("$buildDir/libs")
   inputs.dir("$projectDir/support/jpackage/linux")
   outputs.file(ext.get("debFilename") as String)
   doLast {
      if (OperatingSystem.current().isLinux) {
         val parameters = ArrayList<String>(ext.get("sharedParameters") as ArrayList<String>)
         parameters.addAll(ext.get("linuxParameters") as ArrayList<String>)
         val processBuilder1 = ProcessBuilder()
         processBuilder1.command(parameters)
         val process1 = processBuilder1.start()
         if (process1.waitFor() != 0) {
            throw GradleException("Error while creating deb package")
         }
      }
   }
}

tasks.register("createRpm") {
   group = "build"
   description = "Makes the Linux platform specific packages"
   dependsOn("shadowJar", "createDistDir")
   inputs.dir("$buildDir/libs")
   inputs.dir("$projectDir/support/jpackage/linux")
   outputs.file(ext.get("rpmFilename") as String)
   doLast {
      if (OperatingSystem.current().isLinux) {
         val parameters = ArrayList<String>(ext.get("sharedParameters") as ArrayList<String>)
         parameters.addAll(ext.get("linuxParameters") as ArrayList<String>)
         parameters.addAll(Arrays.asList(
            "--type", "rpm"
         ))
         val processBuilder2 = ProcessBuilder()
         processBuilder2.command(parameters)
         val process2 = processBuilder2.start()
         if (process2.waitFor() != 0) {
            throw GradleException("Error while creating rpm package")
         }
      }
   }
}

tasks.register("createMsi") {
   group = "build"
   description = "Makes the Windows platform specific package"
   dependsOn("shadowJar", "createDistDir")
   inputs.dir("$buildDir/libs")
   inputs.dir("$projectDir/support/jpackage/windows")
   outputs.file(ext.get("msiFilename") as String)
   doLast {
      if (OperatingSystem.current().isWindows) {
         val parameters = ArrayList<String>(ext.get("sharedParameters") as ArrayList<String>)
         parameters.addAll(Arrays.asList(
            "--name", project.name,
            "--file-associations", "$projectDir/support/jpackage/windows/file.jpackage",
            "--icon", "$projectDir/support/jpackage/windows/Logisim-evolution.ico",
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
            throw GradleException("Error while creating msi package")
         }
      }
   }
}

tasks.register("createApp") {
   group = "build"
   description = "Makes the Mac application"
   dependsOn("shadowJar", "createDistDir")
   inputs.dir("$buildDir/libs")
   inputs.dir("$projectDir/support/jpackage/macos")
   outputs.dir(ext.get("appDirname") as String)
   doLast {
      if (OperatingSystem.current().isMacOsX) {
         val appDirname = ext.get("appDirname") as String
         delete(appDirname)
         val parameters = ArrayList<String>(ext.get("sharedParameters") as ArrayList<String>)
         parameters.addAll(Arrays.asList(
            "--name", ext.get("uppercaseProjectName") as String,
            "--file-associations", "$projectDir/support/jpackage/macos/file.jpackage",
            "--icon", "$projectDir/support/jpackage/macos/Logisim-evolution.icns",
            "--type", "app-image"
         ))
         val processBuilder1 = ProcessBuilder()
         processBuilder1.command(parameters)
         val process1 = processBuilder1.start()
         if (process1.waitFor() != 0) {
            throw GradleException("Error while creating app directory")
         }
         val pListFilename = "$appDirname/Contents/Info.plist"
         val parameters2 = ArrayList<String>(Arrays.asList(
            "awk", "/Unknown/{sub(/Unknown/,\"public.app-category.education\")};{print >\"$buildDir/dist/Info.plist\"};/NSHighResolutionCapable/{print \"  <string>true</string>\" >\"$buildDir/dist/Info.plist\"; print \"  <key>NSSupportsAutomaticGraphicsSwitching</key>\" >\"$buildDir/dist/Info.plist\"}",
            pListFilename
         ))
         val processBuilder2 = ProcessBuilder()
         processBuilder2.command(parameters2)
         val process2 = processBuilder2.start()
         if (process2.waitFor() != 0) {
            throw GradleException("Error while patching Info.plist")
         }
         val parameters3 = ArrayList<String>(Arrays.asList(
            "mv", "$buildDir/dist/Info.plist", pListFilename
         ))
         val processBuilder3 = ProcessBuilder()
         processBuilder3.command(parameters3)
         val process3 = processBuilder3.start()
         if (process3.waitFor() != 0) {
            throw GradleException("Error while moving Info.plist into app")
         }
         val parameters4 = ArrayList<String>(Arrays.asList(
            "codesign", "--remove-signature", appDirname
         ))
         val processBuilder4 = ProcessBuilder()
         processBuilder4.command(parameters4)
         val process4 = processBuilder4.start()
         if (process4.waitFor() != 0) {
            throw GradleException("Error while executing codesign --remove-signature")
         }
      }
   }
}

tasks.register("createDmg") {
   group = "build"
   description = "Makes the Mac dmg package"
   dependsOn("createApp")
   inputs.dir(ext.get("appDirname") as String)
   outputs.file(ext.get("dmgFilename") as String)
   doLast {
      if (OperatingSystem.current().isMacOsX) {
         val parameters1 = ArrayList<String>(Arrays.asList(
            ext.get("jPackageCmd") as String,
            "--type", "dmg",
            "--app-image", ext.get("appDirname") as String,
            "--name", project.name as String,
            "--app-version", project.version as String,
            "--dest", "$buildDir/dist"
         ))
         val processBuilder1 = ProcessBuilder()
         processBuilder1.command(parameters1)
         val process1 = processBuilder1.start()
         if (process1.waitFor() != 0) {
            throw GradleException("Error while creating dmg package")
         }
      }
   }
}

tasks.register("jpackage") {
   group = "build"
   description = "Makes the platform specific packages for the current platform"
   dependsOn("createDeb", "createRpm", "createMsi", "createDmg")
}

tasks {
    compileJava {
        options.compilerArgs = listOf("-Xlint:deprecation", "-Xlint:unchecked", "-Xlint:fallthrough")
    }
    compileTestJava {
        options.compilerArgs = listOf("-Xlint:deprecation", "-Xlint:unchecked", "-Xlint:fallthrough")
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
        System.setProperty( "org.checkstyle.google.suppressionfilter.config", "$projectDir/config/checkstyle/suppressions.xml")
    }
    checkstyleMain {
        source = fileTree("src/main/java")
    }
    checkstyleTest {
        source = fileTree("src/test/java")
    }

}

